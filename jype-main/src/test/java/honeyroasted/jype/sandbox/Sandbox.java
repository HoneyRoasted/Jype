package honeyroasted.jype.sandbox;

import honeyroasted.jype.metadata.signature.JDescriptor;
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
import java.util.Arrays;

public class Sandbox {

    public static void main(String[] args) throws IOException {
        JTypeSystem reflectionSystem = JTypeSystem.RUNTIME_REFLECTION;

        JTypeSystem bytecodeSystem = new JSimpleTypeSystem("BYTECODE_AND_REFLECTION", JTypeCacheFactory.IN_MEMORY_FACTORY,
                new JBinaryLocationClassReferenceResolver(JBinaryClassFinder.rootedIn(Paths.get("jype-main/build/classes/java/main"))),
                JBinaryTypeResolution.BINARY_TYPE_RESOLVERS, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS);

        JClassReference reflectionRef = reflectionSystem.tryResolve(JDescriptor.class);
        System.out.println(reflectionRef.nestMembers());

        JClassReference bytecodeRef = bytecodeSystem.tryResolve(JDescriptor.class);
        System.out.println(bytecodeRef.nestMembers());

        System.out.println(String[].class.getSuperclass());
        System.out.println(Arrays.toString(String[].class.getInterfaces()));
    }

}
