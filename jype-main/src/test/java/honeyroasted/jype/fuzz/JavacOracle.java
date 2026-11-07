package honeyroasted.jype.fuzz;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavacOracle {
    private JavacOracle() {
    }

    public static CompiledWorld compile(TypeWorld world, Path outDir) throws IOException {
        List<String> names = new ArrayList<>(world.fieldNames());
        List<String> types = new ArrayList<>(world.fieldTypes());
        String header = world.source().substring(0, world.source().indexOf("abstract class H"));
        String holderHead = world.source().substring(world.source().indexOf("abstract class H"), world.source().indexOf("{\n", world.source().indexOf("abstract class H")) + 2);

        for (int attempt = 0; attempt < 6; attempt++) {
            StringBuilder sb = new StringBuilder(header).append(holderHead);
            int firstFieldLine = (int) (header + holderHead).chars().filter(c -> c == '\n').count() + 1;
            for (int i = 0; i < names.size(); i++) {
                sb.append("    ").append(types.get(i)).append(" ").append(names.get(i)).append(";\n");
            }
            sb.append("}\n");
            String source = sb.toString();

            Path dir = Files.createDirectories(outDir.resolve("attempt" + attempt));
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
            JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///" + world.packageName().replace('.', '/') + "/World.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            JavacTask task = (JavacTask) compiler.getTask(null, fm, diagnostics, List.of("-d", dir.toString(), "-proc:none", "-Xlint:none", "-nowarn"), null, List.of(file));
            task.parse();
            task.analyze();

            Set<Integer> badFields = new LinkedHashSet<>();
            boolean otherErrors = false;
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    int idx = (int) d.getLineNumber() - firstFieldLine;
                    if (idx >= 0 && idx < names.size()) {
                        badFields.add(idx);
                    } else {
                        otherErrors = true;
                    }
                }
            }
            if (otherErrors) {
                return null;
            }
            if (!badFields.isEmpty()) {
                List<String> newNames = new ArrayList<>();
                List<String> newTypes = new ArrayList<>();
                for (int i = 0; i < names.size(); i++) {
                    if (!badFields.contains(i)) {
                        newNames.add(names.get(i));
                        newTypes.add(types.get(i));
                    }
                }
                names = newNames;
                types = newTypes;
                continue;
            }

            Types javacTypes = Types.instance(((JavacTaskImpl) task).getContext());
            TypeElement holder = task.getElements().getTypeElement(world.holderName());
            Map<String, Type> fieldTypes = new LinkedHashMap<>();
            Map<String, String> fieldSources = new LinkedHashMap<>();
            for (Element e : holder.getEnclosedElements()) {
                if (e instanceof VariableElement ve && ElementFilter.fieldsIn(List.of(e)).size() == 1) {
                    String n = ve.getSimpleName().toString();
                    fieldTypes.put(n, (Type) ve.asType());
                    fieldSources.put(n, types.get(names.indexOf(n)));
                }
            }
            task.generate();
            return new CompiledWorld(source, world.packageName(), dir, javacTypes, fieldTypes, fieldSources);
        }
        return null;
    }
}
