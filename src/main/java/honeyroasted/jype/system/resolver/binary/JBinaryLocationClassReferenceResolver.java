package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassBytecode;
import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.io.IOException;
import java.util.Optional;

public class JBinaryLocationClassReferenceResolver implements JTypeResolver<JClassLocation, JType> {
    private JBinaryClassFinder finder;

    public JBinaryLocationClassReferenceResolver(JBinaryClassFinder finder) {
        this.finder = finder;
    }

    @Override
    public JResolutionResult<JClassLocation, JType> resolve(JTypeSystem system, JClassLocation value) {
        try {
            Optional<byte[]> lookup = this.finder.locate(value);
            return lookup.map(bytes -> JResolutionResult.inherit(value, system.resolve(JClassBytecode.class, JType.class, new JClassBytecode(bytes))))
                    .orElseGet(() -> new JResolutionResult<>("Could not find class binary (file does not exist)", value));
        } catch (IOException e) {
            return new JResolutionResult<>("IO Error occurred while looking up class binary", value, e);
        }
    }
}
