package honeyroasted.jypestub.basic;

import com.fasterxml.jackson.databind.ObjectMapper;
import honeyroasted.jype.system.JSimpleTypeSystem;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jypestub.model.JStubFile;
import honeyroasted.jypestub.model.JStubFolder;
import honeyroasted.jypestub.model.JStubSerialization;
import honeyroasted.jypestub.resolver.JStubClassResolution;

import java.io.IOException;
import java.nio.file.Paths;

public class RunTests {

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = JStubSerialization.buildMapper();
        JStubFolder folder = JStubFolder.read(Paths.get("jype-stub/src/test/resources/"), mapper, true);

        JTypeSystem system = new JSimpleTypeSystem("STUB_TESTS", JTypeCacheFactory.IN_MEMORY_FACTORY,
                JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS,
                JStubClassResolution.stubResolvers(folder));

        for (JStubFile file : folder.files()) {
            System.out.println(file.name() + ": " + file.runTestsReport(system));
            System.out.println("-".repeat(100));
        }
    }

}
