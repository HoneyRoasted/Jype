package honeyroasted.jype.type;

import honeyroasted.jype.model.PossiblyUnmodifiable;
import honeyroasted.jype.system.TypeSystem;

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
