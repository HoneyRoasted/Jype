package honeyroasted.jype.fuzz;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

import java.nio.file.Path;
import java.util.Map;

public record CompiledWorld(String source, String packageName, Path classesDir, Types types, Map<String, Type> fieldTypes, Map<String, String> fieldSources) {
}
