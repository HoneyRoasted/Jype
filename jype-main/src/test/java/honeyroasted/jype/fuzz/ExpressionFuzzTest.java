package honeyroasted.jype.fuzz;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("fuzz")
public class ExpressionFuzzTest {
    private static final String NL = String.valueOf((char) 10);
    private static final Map<String, String> CLASS_NAMES = Map.of(
            "String", "java.lang.String",
            "Integer", "java.lang.Integer",
            "Object", "java.lang.Object",
            "StringBuilder", "java.lang.StringBuilder",
            "java.util.ArrayList", "java.util.ArrayList",
            "Runnable", "java.lang.Runnable",
            "Comparable", "java.lang.Comparable"
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
            ExpressionWorld world = new ExpressionWorldGenerator(worldSeed).generate(pkg, 80);
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
                JMethodReference[] currentProbe = new JMethodReference[1];
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
                    public JMethodReference probeMethod() {
                        return currentProbe[0];
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
                        currentProbe[0] = probe.instance() ? null : system.tryResolve(host.getDeclaredMethod("staticContext"));
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
                        record(mismatches, tally, new FuzzMismatch(category, probe.targetSource() + " x = " + probe.expression().source(), javacMsg, actual.render(), "  world " + worldSeed + "\n" + declarations(compiled.source())));
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

    private record Attempt(boolean crashed, Set<Integer> badDecls, Set<Integer> rejected, Map<Integer, String> messages, boolean headerError, boolean ok) {
    }

    private static Attempt attempt(ExpressionWorld world, List<String> decls, List<Probe> probes, Path out) throws Exception {
        StringBuilder sb = new StringBuilder(world.header()).append("abstract class M {").append(NL);
        int firstDecl = (int) sb.chars().filter(c -> c == '\n').count() + 1;
        decls.forEach(d -> sb.append(d).append(NL));
        int firstProbe = firstDecl + decls.size();
        probes.forEach(p -> sb.append(p.declaration()).append(NL));
        int sentinelLine = firstProbe + probes.size();
        if (!probes.isEmpty()) {
            sb.append("    static void sentinel() { int x = \"sentinel\"; }").append(NL);
        }
        sb.append("}").append(NL);
        String source = sb.toString();
        Files.createDirectories(out);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///" + world.packageName() + "/M.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        java.io.StringWriter log = new java.io.StringWriter();
        boolean ok;
        boolean crashed = false;
        try {
            ok = Boolean.TRUE.equals(compiler.getTask(log, compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8), diagnostics,
                    List.of("-d", out.toString(), "-proc:none", "-Xlint:none", "-nowarn", "-Xmaxerrs", "100000"), null, List.of(file)).call());
        } catch (RuntimeException e) {
            ok = false;
            crashed = true;
        }
        if (log.toString().contains("exception has occurred in the compiler")) {
            crashed = true;
        }
        Set<Integer> badDecls = new LinkedHashSet<>();
        Set<Integer> rejected = new LinkedHashSet<>();
        Map<Integer, String> messages = new HashMap<>();
        boolean headerError = false;
        boolean sentinelSeen = false;
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            int line = (int) d.getLineNumber();
            if (d.getCode() != null && d.getCode().contains("preview") && probes.size() > 1) {
                crashed = true;
            }
            if (line >= firstProbe && line < firstProbe + probes.size()) {
                int idx = probes.get(line - firstProbe).index();
                rejected.add(idx);
                messages.putIfAbsent(idx, d.getMessage(null).lines().findFirst().orElse(""));
            } else if (line >= firstDecl && line < firstProbe) {
                badDecls.add(line - firstDecl);
            } else if (line == sentinelLine && !probes.isEmpty()) {
                sentinelSeen = true;
            } else {
                headerError = true;
            }
        }
        if (!probes.isEmpty() && !sentinelSeen) {
            crashed = true;
        }
        return new Attempt(crashed, badDecls, rejected, messages, headerError, ok);
    }

    private static Compiled compile(ExpressionWorld world, Path dir) throws Exception {
        List<String> decls = new ArrayList<>(world.declarations());
        List<Probe> probes = new ArrayList<>(world.probes());
        Path declDir = dir.resolve("decls");
        for (int i = 0; i < 8; i++) {
            Attempt a = attempt(world, decls, List.of(), declDir.resolve("a" + i));
            if (a.headerError() || a.crashed()) {
                return null;
            }
            if (a.badDecls().isEmpty()) {
                declDir = declDir.resolve("a" + i);
                break;
            }
            List<String> removedHolders = new ArrayList<>();
            List<String> next = new ArrayList<>();
            for (int j = 0; j < decls.size(); j++) {
                if (a.badDecls().contains(j)) {
                    String d = decls.get(j).trim();
                    removedHolders.add(d.substring(d.lastIndexOf(' ') + 1).replace(";", ""));
                } else {
                    next.add(decls.get(j));
                }
            }
            decls = next;
            probes.removeIf(p -> removedHolders.contains(p.targetHolder()) || removedHolders.stream().anyMatch(h -> p.expression().toString().contains("holderField=" + h + "]")));
            if (i == 7) {
                return null;
            }
        }
        Set<Integer> rejected = new LinkedHashSet<>();
        Map<Integer, String> messages = new HashMap<>();
        List<Probe> kept = new ArrayList<>();
        probeBatch(world, decls, probes, dir.resolve("p"), rejected, messages, kept);
        StringBuilder sb = new StringBuilder(world.header()).append("abstract class M {").append(NL);
        decls.forEach(d -> sb.append(d).append(NL));
        return new Compiled(sb.toString(), declDir, rejected, kept, messages);
    }

    private static void probeBatch(ExpressionWorld world, List<String> decls, List<Probe> probes, Path out, Set<Integer> rejected, Map<Integer, String> messages, List<Probe> kept) throws Exception {
        if (probes.isEmpty()) {
            return;
        }
        Attempt a = attempt(world, decls, probes, out);
        if (a.crashed()) {
            if (probes.size() == 1) {
                return;
            }
            int half = probes.size() / 2;
            probeBatch(world, decls, probes.subList(0, half), out.resolve("l"), rejected, messages, kept);
            probeBatch(world, decls, probes.subList(half, probes.size()), out.resolve("r"), rejected, messages, kept);
            return;
        }
        rejected.addAll(a.rejected());
        messages.putAll(a.messages());
        kept.addAll(probes);
    }

    private static String declarations(String source) {
        StringBuilder sb = new StringBuilder();
        for (String line : source.split("\n")) {
            if (line.contains("static void probe")) {
                break;
            }
            if (line.contains("(") || line.contains("interface")) {
                sb.append("    ").append(line.trim()).append("\n");
            }
        }
        return sb.toString();
    }

    private static void record(List<FuzzMismatch> mismatches, Map<String, Integer> tally, FuzzMismatch mismatch) {
        tally.merge(mismatch.category(), 1, Integer::sum);
        mismatches.add(mismatch);
    }
}
