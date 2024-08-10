package honeyroasted.jype.type.signature;

import honeyroasted.jype.location.GenericDeclarationLocation;

public record SignatureContext(Signature signature, GenericDeclarationLocation containing) {

}
