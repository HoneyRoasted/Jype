package honeyroasted.jype.basic;

import honeyroasted.almonds.SimpleName;
import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.system.resolver.binary.JBinaryClassFinder;
import honeyroasted.jype.system.resolver.binary.JBinaryLocationClassReferenceResolver;
import honeyroasted.jype.system.resolver.binary.JBinaryTypeResolution;
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jype.type.JClassReference;

import java.io.IOException;
import java.nio.file.Paths;

public class ReadBytecodeTest {

    public static void main(String[] args) throws IOException {
        JTypeSystem system = new JSimpleTypeSystem("BINARY_TEST", JTypeCacheFactory.IN_MEMORY_FACTORY,
                new JBinaryLocationClassReferenceResolver(JBinaryClassFinder.rootedIn(Paths.get("build/classes/java/main"))),
                JBinaryTypeResolution.BINARY_TYPE_RESOLVERS, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS);

        JClassReference ref = system.tryResolve(JClassLocation.of(JTypeResolver.class));

        JClassReference fromReflection = JTypeSystem.RUNTIME_REFLECTION.tryResolve(JClassLocation.of(JTypeResolver.class));

        System.out.println(ref.equals(fromReflection));
        System.out.println();
        ref.declaredMethods().stream().map(SimpleName::simpleName).forEach(System.out::println);
        System.out.println();
        ref.declaredFields().stream().map(SimpleName::simpleName).forEach(System.out::println);
    }

}
