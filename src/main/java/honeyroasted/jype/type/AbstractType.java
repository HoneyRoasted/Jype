package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;

public abstract class AbstractType implements TypeConcrete {
    private TypeSystem typeSystem;

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeConcrete type &&
                this.flatten().equalsExactly(type.flatten());
    }

    @Override
    public int hashCode() {
        return this.flatten().hashCodeExactly();
    }

}
