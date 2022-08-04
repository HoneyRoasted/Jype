package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Type;
import honeyroasted.jype.type.TypeDeclaration;

public interface TypeResolutionStrategy {

    Type resolve(Object type);

    TypeDeclaration resolveDeclaration(Object type);

}
