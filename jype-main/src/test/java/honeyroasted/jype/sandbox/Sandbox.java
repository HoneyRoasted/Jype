package honeyroasted.jype.sandbox;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JClassName;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.resolver.binary.JBinaryClassFinder;
import honeyroasted.jype.system.resolver.binary.JBinaryLocationClassReferenceResolver;
import honeyroasted.jype.system.resolver.binary.JBinaryTypeResolution;
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jype.type.JClassReference;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Sandbox {
    private static List<Class> classes = new ArrayList<>();

    private static Object object = new Object(){{
        classes.add(getClass());
    }};

    static class InnerClass {
        static class InnerInnerClass {

        }
    }

    static {
        new Object(){{
            classes.add(getClass());
        }};

        class StaticInitTest {

        }
        classes.add(StaticInitTest.class);

        classes.add(InnerClass.class);
        classes.add(InnerClass.InnerInnerClass.class);
    }

    public static void main(String[] args) throws IOException {
        JTypeSystem reflectionSystem = JTypeSystem.RUNTIME_REFLECTION;

        JTypeSystem bytecodeSystem = new JSimpleTypeSystem("BYTECODE_AND_REFLECTION", JTypeCacheFactory.IN_MEMORY_FACTORY,
                new JBinaryLocationClassReferenceResolver(JBinaryClassFinder.rootedIn(Paths.get("jype-main/build/classes/java/test"))),
                JBinaryTypeResolution.BINARY_TYPE_RESOLVERS, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS);

        new Object() {{
            classes.add(getClass());
        }};

        class Test {

        }

        classes.add(Test.class);

        for (Class cls : classes) {
            JClassReference ref = bytecodeSystem.tryResolve(JClassLocation.of(cls));
            System.out.println("-------------- " + cls + " -------------- ");
            System.out.println("Expected name: " + JClassName.of(cls));
            System.out.println("Determi. name: " + ref.namespace().name());
            System.out.println("-------------- -------------- --------------");
        }
    }

    private static void test() {
        doAThing(a -> 1); //Why does this work??
    }

    private static <F extends Function<String, Integer>> void doAThing(F func) {

    }

}
