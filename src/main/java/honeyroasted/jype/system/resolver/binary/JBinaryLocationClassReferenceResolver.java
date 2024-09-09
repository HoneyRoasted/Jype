package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassBytecode;
import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
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
    public Optional<? extends JType> resolve(JTypeSystem system, JClassLocation value) {
        try {
            return this.finder.locate(value).flatMap(bytes -> system.resolve(JClassBytecode.class, JType.class, new JClassBytecode(bytes)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
