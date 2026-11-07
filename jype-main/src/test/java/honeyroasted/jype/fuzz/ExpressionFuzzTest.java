package honeyroasted.jype.fuzz;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fuzz")
public class ExpressionFuzzTest {
    private static final Map<String, String> CLASS_NAMES = Map.of(
            "String", "java.lang.String",
            "Integer", "java.lang.Integer",
            "Object", "java.lang.Object",
            "StringBuilder", "java.lang.StringBuilder",
            "java.util.ArrayList", "java.util.ArrayList"
    );

    private record Compiled(String source, Path dir, Set<Integer> rejected, List<Probe> probes, Map<Integer, String> messages) {
    }

    @Test
    public void expressionsMatchJavac() throws Exception {
        long seed = Long.parseLong(System.getProperty("fuzz.seed", "1"));
        int count = Integer.parseInt(System.getProperty("fuzz.count", "20"));
        Path out = Path.of(System.getProperty("fuzz.out", "build/fuzz-failures"));
        Files.createDirectories(out);
        Path work = Files.createTempDirectory("jype-efuzz");

        List<FuzzMismatch> mismatches = new ArrayList<>();
        Map<String, Integer> tally = new TreeMap<>();
        int checks = 0;
        int accepted = 0;
        for (int w = 0; w < count; w++) {
            long worldSeed = seed * 1_000_003L + w;
            String pkg = "fe" + Long.toHexString(worldSeed).replace('-', 'n');
            ExpressionWorld world = new ExpressionWorldGenerator(worldSeed).generate(pkg, 40);
            Compiled compiled = compile(world, work.resolve("w" + w));
            if (compiled == null) {
                continue;
            }
            try (URLClassLoader loader = new URLClassLoader(new URL[]{compiled.dir().toUri().toURL()}, getClass().getClassLoader());
                 TimedRunner runner = new TimedRunner(8000, loader)) {
                Class<?> host = loader.loadClass(pkg + ".M");
                JTypeSystem system = new JSimpleTypeSystem("EFUZZ_" + worldSeed);
                Outcome<JClassReference> hostRef = runner.run(() -> system.tryResolve(host));
                if (hostRef.value() == null) {
                    record(mismatches, tally, new FuzzMismatch("host-resolve", pkg, "ok", hostRef.render(), null));
                    continue;
                }
                Map<String, JType> holderCache = new HashMap<>();
                FuzzContext ctx = new FuzzContext() {
                    @Override
                    public JTypeSystem system() {
                        return system;
                    }

                    @Override
                    public JClassReference host() {
                        return hostRef.value();
                    }

                    @Override
                    public JType typeOf(String holderField) {
                        return holderCache.computeIfAbsent(holderField, f -> {
                            try {
                                return system.tryResolve(host.getDeclaredField(f).getGenericType());
                            } catch (NoSuchFieldException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                    }

                    @Override
                    public JClassReference classNamed(String name) {
                        if (name.equals("M")) {
                            return hostRef.value();
                        }
                        try {
                            return system.tryResolve(Class.forName(CLASS_NAMES.getOrDefault(name, name), false, loader));
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                };

                for (Probe probe : compiled.probes()) {
                    boolean expected = !compiled.rejected().contains(probe.index());
                    Outcome<Boolean> actual = runner.run(() -> {
                        JType target = ctx.typeOf(probe.targetHolder());
                        JExpressionInformation expr = probe.expression().toJype(ctx);
                        ConstraintTree tree = system.operations().inferenceSolver()
                                .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target))
                                .solve();
                        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
                    });
                    checks++;
                    if (expected) {
                        accepted++;
                    }
                    if (actual.value() == null || actual.value() != expected) {
                        String category = actual.value() == null ? (actual.timedOut() ? "timeout" : "error") : (expected ? "javac-accepts" : "javac-rejects");
                        String javacMsg = expected ? "accepts" : "rejects: " + compiled.messages().getOrDefault(probe.index(), "");
                        record(mismatches, tally, new FuzzMismatch(category, probe.targetSource() + " x = " + probe.expression().source(), javacMsg, actual.render(), "  world " + worldSeed));
                    }
                }
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("seed=").append(seed).append(" worlds=").append(count).append(" checks=").append(checks).append(" javacAccepted=").append(accepted).append(" mismatches=").append(mismatches.size()).append("\n");
        tally.forEach((k, v) -> report.append("  ").append(k).append(": ").append(v).append("\n"));
        report.append("\n");
        mismatches.forEach(m -> report.append(m.render()).append("\n\n"));
        Path file = out.resolve("expressions-" + seed + ".txt");
        Files.writeString(file, report.toString());
        System.out.println(report.substring(0, Math.min(report.length(), 3000)));
        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches, see " + file);
    }

    private static Compiled compile(ExpressionWorld world, Path dir) throws Exception {
        List<String> decls = new ArrayList<>(world.declarations());
        List<Probe> probes = new ArrayList<>(world.probes());
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder sb = new StringBuilder(world.header()).append("abstract class M {\n");
            int firstDecl = (int) sb.chars().filter(c -> c == '\n').count() + 1;
            decls.forEach(d -> sb.append(d).append("\n"));
            int firstProbe = firstDecl + decls.size();
            probes.forEach(p -> sb.append(p.declaration()).append("\n"));
            sb.append("}\n");
            String source = sb.toString();

            Path attemptDir = Files.createDirectories(dir.resolve("a" + attempt));
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///" + world.packageName() + "/M.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            Boolean ok = compiler.getTask(null, compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8), diagnostics,
                    List.of("-d", attemptDir.toString(), "-proc:none", "-Xlint:none", "-nowarn", "-Xmaxerrs", "100000"), null, List.of(file)).call();

            Set<Integer> badDecls = new LinkedHashSet<>();
            Set<Integer> rejected = new LinkedHashSet<>();
            Map<Integer, String> messages = new HashMap<>();
            boolean headerError = false;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR) {
                    continue;
                }
                int line = (int) d.getLineNumber();
                if (line >= firstProbe && line < firstProbe + probes.size()) {
                    int idx = probes.get(line - firstProbe).index();
                    rejected.add(idx);
                    messages.putIfAbsent(idx, d.getMessage(null).lines().findFirst().orElse(""));
                } else if (line >= firstDecl && line < firstProbe) {
                    badDecls.add(line - firstDecl);
                } else {
                    headerError = true;
                }
            }
            if (headerError) {
                return null;
            }
            if (!badDecls.isEmpty()) {
                List<String> removedHolders = new ArrayList<>();
                List<String> next = new ArrayList<>();
                for (int i = 0; i < decls.size(); i++) {
                    if (badDecls.contains(i)) {
                        String d = decls.get(i).trim();
                        String last = d.substring(d.lastIndexOf(' ') + 1).replace(";", "");
                        removedHolders.add(last);
                    } else {
                        next.add(decls.get(i));
                    }
                }
                decls = next;
                probes.removeIf(p -> removedHolders.contains(p.targetHolder()) || removedHolders.stream().anyMatch(h -> p.expression().toString().contains("holderField=" + h + "]")));
                continue;
            }
            if (!rejected.isEmpty() && !Boolean.TRUE.equals(ok)) {
                Path okDir = Files.createDirectories(dir.resolve("ok"));
                StringBuilder ksb = new StringBuilder(world.header()).append("abstract class M {\n");
                decls.forEach(d -> ksb.append(d).append("\n"));
                ksb.append("}\n");
                String ksrc = ksb.toString();
                JavaFileObject kfile = new SimpleJavaFileObject(URI.create("string:///" + world.packageName() + "/M.java"), JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return ksrc;
                    }
                };
                Boolean kok = compiler.getTask(null, compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8), null,
                        List.of("-d", okDir.toString(), "-proc:none", "-Xlint:none", "-nowarn"), null, List.of(kfile)).call();
                if (!Boolean.TRUE.equals(kok)) {
                    return null;
                }
                return new Compiled(source, okDir, rejected, probes, messages);
            }
            return new Compiled(source, attemptDir, rejected, probes, messages);
        }
        return null;
    }

    private static void record(List<FuzzMismatch> mismatches, Map<String, Integer> tally, FuzzMismatch mismatch) {
        tally.merge(mismatch.category(), 1, Integer::sum);
        mismatches.add(mismatch);
    }
}
