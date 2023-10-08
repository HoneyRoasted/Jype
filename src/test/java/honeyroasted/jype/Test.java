package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.AssignabilityTypeSolver;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {
        TypeSystem system = new TypeSystem();
        ClassType type = (ClassType) system.resolve(Foo.Bar.class).get();
        System.out.println(type);
        ClassType type2 = (ClassType) system.resolve(new TypeToken<Foo.Bar<Integer>>(){}).get();
        System.out.println(type2);

        System.out.println(Modifier.isStatic(type.modifiers()));
    }

    private static String fileName(Class<?> cls) {
        return "build/classes/java/test/" + cls.getName().replace('.', '/') + ".class";
    }

    private static void print(String file) throws IOException {
        TraceClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.out));
        ClassReader reader = new ClassReader(Files.readAllBytes(Paths.get(file)));
        reader.accept(tracer, 0);
        System.out.println("#############################################################################");
        System.out.println("#############################################################################");
    }

}
