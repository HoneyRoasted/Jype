package honeyroasted.jype.fuzz;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fuzz")
public class RelationFuzzTest {

    private record Relation(String name, BiFunction<Types, Type[], Boolean> javac, BiFunction<JTypeSystem, JType[], Boolean> jype) {
    }

    private static final List<Relation> RELATIONS = List.of(
            new Relation("subtype", (t, p) -> t.isSubtype(p[0], p[1]), (s, p) -> s.operations().isSubtype(p[0], p[1])),
            new Relation("same", (t, p) -> t.isSameType(p[0], p[1]), (s, p) -> p[0].typeEquals(p[1])),
            new Relation("assign", (t, p) -> t.isAssignable(p[0], p[1]), (s, p) -> s.operations().isCompatible(p[0], p[1], JTypeConstraints.Compatible.Context.ASSIGNMENT)),
            new Relation("cast", (t, p) -> t.isCastable(p[0], p[1]), (s, p) -> s.operations().isCompatible(p[0], p[1], JTypeConstraints.Compatible.Context.EXPLICIT_CAST))
    );

    @Test
    public void relationsMatchJavac() throws Exception {
        long seed = Long.parseLong(System.getProperty("fuzz.seed", "1"));
        int count = Integer.parseInt(System.getProperty("fuzz.count", "20"));
        Path out = Path.of(System.getProperty("fuzz.out", "build/fuzz-failures"));
        Files.createDirectories(out);
        Path work = Files.createTempDirectory("jype-fuzz");

        List<FuzzMismatch> mismatches = new ArrayList<>();
        Map<String, Integer> tally = new TreeMap<>();
        int checks = 0;
        for (int w = 0; w < count; w++) {
            long worldSeed = seed * 1_000_003L + w;
            TypeWorld world = new TypeWorldGenerator(worldSeed).generate("fz" + Long.toHexString(worldSeed).replace('-', 'n'), 24);
            CompiledWorld compiled = JavacOracle.compile(world, work.resolve("w" + w));
            if (compiled == null) {
                continue;
            }
            checks += this.checkWorld(compiled, worldSeed, mismatches, tally);
        }

        StringBuilder report = new StringBuilder();
        report.append("seed=").append(seed).append(" worlds=").append(count).append(" checks=").append(checks).append(" mismatches=").append(mismatches.size()).append("\n");
        tally.forEach((k, v) -> report.append("  ").append(k).append(": ").append(v).append("\n"));
        report.append("\n");
        mismatches.forEach(m -> report.append(m.render()).append("\n\n"));
        Path file = out.resolve("relations-" + seed + ".txt");
        Files.writeString(file, report.toString());
        System.out.println(report.substring(0, Math.min(report.length(), 4000)));
        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches, see " + file);
    }

    private int checkWorld(CompiledWorld compiled, long worldSeed, List<FuzzMismatch> mismatches, Map<String, Integer> tally) throws IOException, ReflectiveOperationException {
        int checks = 0;
        try (URLClassLoader loader = new URLClassLoader(new URL[]{compiled.classesDir().toUri().toURL()}, RelationFuzzTest.class.getClassLoader());
             TimedRunner runner = new TimedRunner(4000, loader)) {
            Class<?> holder = loader.loadClass(compiled.packageName() + ".H");
            JTypeSystem system = new JSimpleTypeSystem("FUZZ_" + worldSeed);

            Map<String, JType> jypeTypes = new LinkedHashMap<>();
            for (String name : compiled.fieldTypes().keySet()) {
                java.lang.reflect.Type reflect = holder.getDeclaredField(name).getGenericType();
                Outcome<JType> resolved = runner.run(() -> system.tryResolve(reflect));
                if (resolved.value() == null) {
                    record(mismatches, tally, new FuzzMismatch("resolve", compiled.fieldSources().get(name), "ok", resolved.render(), null));
                    continue;
                }
                jypeTypes.put(name, resolved.value());
            }

            List<String> names = new ArrayList<>(jypeTypes.keySet());
            Random random = new Random(worldSeed);
            int pairs = Math.min(names.size() * names.size(), 120);
            for (int i = 0; i < pairs; i++) {
                String a = names.get(random.nextInt(names.size()));
                String b = names.get(random.nextInt(names.size()));
                Type ja = compiled.fieldTypes().get(a);
                Type jb = compiled.fieldTypes().get(b);
                JType ta = jypeTypes.get(a);
                JType tb = jypeTypes.get(b);
                for (Relation relation : RELATIONS) {
                    boolean expected = relation.javac().apply(compiled.types(), new Type[]{ja, jb});
                    Outcome<Boolean> actual = runner.run(() -> relation.jype().apply(system, new JType[]{ta, tb}));
                    checks++;
                    if (actual.value() == null || actual.value() != expected) {
                        String desc = relation.name() + "(" + compiled.fieldSources().get(a) + ", " + compiled.fieldSources().get(b) + ")";
                        record(mismatches, tally, new FuzzMismatch(relation.name() + (actual.value() == null ? "-" + (actual.timedOut() ? "timeout" : "error") : ""), desc, String.valueOf(expected), actual.render(), "  world " + worldSeed + ":\n" + indent(compiled.source())));
                    }
                }
            }
        }
        return checks;
    }

    private static void record(List<FuzzMismatch> mismatches, Map<String, Integer> tally, FuzzMismatch mismatch) {
        tally.merge(mismatch.category(), 1, Integer::sum);
        if (mismatches.stream().noneMatch(m -> m.description().equals(mismatch.description()) && m.category().equals(mismatch.category()))) {
            mismatches.add(mismatch);
        }
    }

    private static String indent(String source) {
        return source.lines().map(l -> "    " + l).reduce((x, y) -> x + "\n" + y).orElse("");
    }
}
