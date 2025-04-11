package honeyroasted.jype.type;

import honeyroasted.jype.metadata.location.JGenericDeclarationLocation;

import java.util.List;
import java.util.Optional;

public interface JGenericDeclaration extends JType {

    JGenericDeclarationLocation genericDeclarationLocation();

    List<JVarType> typeParameters();

    void setTypeParameters(List<JVarType> typeParameters);

    Optional<JVarType> resolveVarType(String name);

}
