package honeyroasted.jype;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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

        printName(anonymousClassInMethod);
        printName(insideStatic2);
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
        System.out.println("Name: " + Name.nameOf(cls));
        System.out.println("Location: " + Name.locationOf(cls));
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
