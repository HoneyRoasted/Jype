package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JStringParseException;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

public class JDeclaredSignatureTypeResolver implements JTypeResolver<JSignature.Declared, JType> {
    @Override
    public JResolutionResult<JSignature.Declared, JType> resolve(JTypeSystem system, JSignature.Declared value) {
        try {
            return new JResolutionResult<>(JBinaryTypeResolution.resolveTypeSig(system, value.containing(), value.signature()), value);
        } catch (JStringParseException | JResolutionFailedException | JBinaryLookupException ex) {
            return new JResolutionResult<>("Failed to resolve signature", value, ex);
        }
    }
}
