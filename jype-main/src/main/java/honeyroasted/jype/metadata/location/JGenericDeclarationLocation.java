package honeyroasted.jype.metadata.location;

public sealed interface JGenericDeclarationLocation permits JClassNamespace, JMethodLocation {

    JClassLocation containingClass();

}
