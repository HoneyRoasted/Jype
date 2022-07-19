package honeyroasted.jype.concrete;

import honeyroasted.jype.declaration.TypeParameter;

public class TypeParameterReference implements TypeConcrete {
    private TypeParameter variable;

    public TypeParameterReference(TypeParameter variable) {
        this.variable = variable;
    }

}
