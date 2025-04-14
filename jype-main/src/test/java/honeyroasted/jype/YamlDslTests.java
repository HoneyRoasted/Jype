package honeyroasted.jype;

import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jypestub.model.JStubFolder;
import honeyroasted.jypestub.model.JStubSerialization;
import honeyroasted.jypestub.model.test.JStubTest;
import honeyroasted.jypestub.resolver.JStubClassResolution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class YamlDslTests {
    private static JStubFolder folder;
    private static JTypeSystem system;

    @BeforeAll
    public static void beforeAll() throws IOException {
        folder = JStubFolder.read(Paths.get("src/test/resources/stub_tests"), JStubSerialization.buildMapper(), true);
    }

    @BeforeEach
    public void beforeEach() {
        system = new JSimpleTypeSystem("STUB_TESTS", JTypeCacheFactory.IN_MEMORY_FACTORY,
                JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS,
                JStubClassResolution.stubResolvers(folder));
    }

    @ParameterizedTest
    @MethodSource("stubTestProvider")
    public void runTest(Map.Entry<String, JStubTest> test) {
        JStubTest.Result result = test.getValue().test(system);
        assertTrue(result.result(), () -> buildMessage(test.getKey(), result));
    }

    private static String buildMessage(String name, JStubTest.Result result) {
        return name + ": " + result + "\n\n\nCONSTRAINT TREE:\n-----------------\n" +
                result.tree().toString(true);
    }

    private static Stream<Arguments> stubTestProvider() {
        return folder.files().stream().flatMap(file -> file.tests().entrySet().stream())
                .map(Arguments::of);
    }

}
