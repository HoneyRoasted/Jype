package honeyroasted.jype.metadata;

import honeyroasted.jype.metadata.location.JGenericDeclarationLocation;

public record JDeclaredSignature(JSignature signature, JGenericDeclarationLocation location) {
    @Override
    public String toString() {
        return this.signature + " DECLARED AT " + this.location;
    }
}
