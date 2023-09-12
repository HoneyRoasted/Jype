package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;

public abstract class AbstractType implements Type {
    private TypeSystem typeSystem;

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
