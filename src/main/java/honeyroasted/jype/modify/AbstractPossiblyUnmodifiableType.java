package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

public abstract class AbstractPossiblyUnmodifiableType extends PossiblyUnmodifiable implements Type {
    private TypeSystem typeSystem;

    public AbstractPossiblyUnmodifiableType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
