package honeyroasted.jypestub;

import com.fasterxml.jackson.databind.ObjectMapper;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jypestub.model.JStubFolder;
import honeyroasted.jypestub.model.JStubSerialization;
import honeyroasted.jypestub.resolver.JStubClassResolution;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class StubTestTest {

    @Test
    public void parseAndRunStubTests() throws IOException {
        ObjectMapper mapper = JStubSerialization.buildMapper();
        JStubFolder folder = JStubFolder.read(Paths.get("src/test/resources/stub_tests/pass"), mapper, true);

        JTypeSystem system = new JSimpleTypeSystem("STUB_TESTS", JTypeCacheFactory.IN_MEMORY_FACTORY,
                JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS,
                JStubClassResolution.stubResolvers(folder));

        folder.files().forEach(file -> file.runTests(system).forEach((key, result) -> assertTrue(result.result(), key + " failed when it should have passed")));
    }

}
