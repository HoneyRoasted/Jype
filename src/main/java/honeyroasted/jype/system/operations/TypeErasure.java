package honeyroasted.jype.system.operations;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.operations.result.TypeResult;
import honeyroasted.jype.system.operations.result.TypeResultBuilder;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;

public class TypeErasure extends AbstractTypeOperation<TypeConcrete> implements TypeOperation<TypeConcrete> {

    public TypeErasure(TypeConcrete type) {
        super(String.format("Erasure of {%s}.", type), type);
    }

    @Override
    protected TypeResult<TypeConcrete> result() {
        TypeResultBuilder<TypeConcrete> trb = TypeResult.builder(this);

        TypeConcrete type = this.type();
        if (type instanceof TypeParameterized prm) {
            trb.value(prm.declaration().withArguments());
        } else if (type instanceof TypeArray arr) {
            TypeErasure child = new TypeErasure(arr.element());
            trb.andCause(child);
            trb.value(t -> new TypeArray(arr.typeSystem(), child.perform().value()));
        } else if (type instanceof TypeParameter param) {
            TypeConcrete leftmost;
            if (param.bound() instanceof TypeAnd and) {
                leftmost = and.types().isEmpty() ? and : and.types().iterator().next();
            } else {
                leftmost = param.bound();
            }
            TypeErasure child = new TypeErasure(leftmost);
            trb.andCause(child);
            trb.value(t -> child.perform().value());
        } else {
            trb.value(type);
        }

        return trb.build();
    }
}
