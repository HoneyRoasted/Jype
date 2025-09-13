package honeyroasted.jype.type;

import honeyroasted.jype.metadata.location.JClassNamespace;

import java.util.Optional;

public interface JReferencableType extends JType {

    Optional<JClassNamespace> classNamespace();

}
