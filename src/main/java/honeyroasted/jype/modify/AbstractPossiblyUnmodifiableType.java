package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

public abstract sealed class AbstractPossiblyUnmodifiableType extends PossiblyUnmodifiable implements Type permits honeyroasted.jype.type.ClassReference, honeyroasted.jype.type.ClassType, honeyroasted.jype.type.MethodReference, honeyroasted.jype.type.MethodType, honeyroasted.jype.type.VarType {
    private TypeSystem typeSystem;

    public AbstractPossiblyUnmodifiableType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
