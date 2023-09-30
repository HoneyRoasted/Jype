package honeyroasted.jype.type;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Objects;

public final class ArrayType extends AbstractType {
    private Type component;

    public ArrayType(TypeSystem typeSystem, Type component) {
        super(typeSystem);
        this.component = component;
    }

    public Type component() {
        return this.component;
    }

    public int depth() {
        if (this.component instanceof ArrayType aType) {
            return 1 + aType.depth();
        }
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return Objects.equals(component, arrayType.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component);
    }

    @Override
    public String toString() {
        return this.component + "[]";
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitArrayType(this, context);
    }
}
