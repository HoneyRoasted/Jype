package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.Type;

import java.util.Objects;

public final class ArrayTypeImpl extends AbstractPossiblyUnmodifiableType implements ArrayType {
    private Type component;

    public ArrayTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public Type component() {
        return this.component;
    }

    @Override
    public void setComponent(Type component) {
        super.checkUnmodifiable();
        this.component = component;
    }

    @Override
    public int depth() {
        if (this.component instanceof ArrayType aType) {
            return 1 + aType.depth();
        }
        return 1;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ArrayType)) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(component, arrayType.component());
    }

    @Override
    public int hashCode() {
        return Objects.hash(component);
    }

    @Override
    public String toString() {
        return this.component + "[]";
    }

}
