package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.Type;

import java.util.Optional;
import java.util.Set;

public final class ArrayTypeImpl extends AbstractPossiblyUnmodifiableType implements ArrayType {
    private Type component;

    public ArrayTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        ArrayType copy = new ArrayTypeImpl(this.typeSystem());
        copy.setComponent(this.component.copy());
        return (T) copy;
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
    public boolean equals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;
        seen = Type.concat(seen, this);

        if (other instanceof ArrayType at) {
            return Type.equals(this.component(), at.component(), seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.hashCode(this.component(), seen);
    }

}
