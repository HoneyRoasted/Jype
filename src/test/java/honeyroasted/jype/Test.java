package honeyroasted.jype;

import honeyroasted.jype.model.name.ClassLocation;
import honeyroasted.jype.model.name.ClassName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Test {
    static Class<?> insideStatic;
    static Class<?> insideMethod;
    static Class<?> insideConstructor;
    static Class<?> insideInitializer;
    static Class<?> insideStatic2;

    static {
        class InsideStatic {

        }
        insideStatic = InsideStatic.class;
    }

    static {
        class InsideStatic {

        }
        insideStatic2 = InsideStatic.class;
    }

    {
        class InsideInitializer {

        }
        insideInitializer = InsideInitializer.class;
    }

    public Test() {
        class InsideConstructor {

        }
        insideConstructor = InsideConstructor.class;
    }

    public static void main(String[] args) throws IOException {
        Class<?> anonymousClassInMethod = new Object(){}.getClass();

        test();
        new Test();

        printName(int.class);
        printName(String[][][].class);
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

    private static void printName(Class<?> cls) {
        System.out.println("Name: " + ClassName.of(cls));
        System.out.println("Location: " + ClassLocation.of(cls));
        System.out.println("------------------------------------");
    }

    public static void test() {
        class InsideMethod {

        }
        insideMethod = InsideMethod.class;
    }

    class InsideClass {

    }

}
