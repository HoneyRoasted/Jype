package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;

public abstract sealed class AbstractPossiblyUnmodifiableType extends AbstractPossiblyUnmodifiable implements Type permits honeyroasted.jype.type.ClassReference, ParameterizedClassType, honeyroasted.jype.type.MethodReference, ParameterizedMethodType, honeyroasted.jype.type.VarType {
    private TypeSystem typeSystem;

    public AbstractPossiblyUnmodifiableType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
