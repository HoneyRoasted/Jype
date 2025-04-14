package honeyroasted.jypestub.resolver;

import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jypestub.model.JStubFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface JStubClassResolution {

    static JBundledTypeResolvers stubResolvers(JStubFolder... folders) {
        return stubResolvers(Arrays.asList(folders));
    }

    static JBundledTypeResolvers stubResolvers(Collection<JStubFolder> folders) {
        List<JTypeResolver<?, ?>> resolvers = new ArrayList<>();
        resolvers.add(new JStubClassReferenceResolver());
        folders.forEach(folder -> resolvers.add(new JStubLocationClassReferenceResolver(folder)));

        return new JBundledTypeResolvers(resolvers);
    }

}
