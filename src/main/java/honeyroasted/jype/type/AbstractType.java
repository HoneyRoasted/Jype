package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;

public abstract class AbstractType implements TypeConcrete {

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
