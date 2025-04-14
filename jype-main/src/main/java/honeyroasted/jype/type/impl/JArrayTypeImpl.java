package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

import java.util.Optional;
import java.util.Set;

public final class JArrayTypeImpl extends JAbstractPossiblyUnmodifiableType implements JArrayType {
    private JType component;

    public JArrayTypeImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JArrayType copy = this.typeSystem().typeFactory().newArrayType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setComponent(this.component.copy(cache));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public JType component() {
        return this.component;
    }

    @Override
    public void setComponent(JType component) {
        super.checkUnmodifiable();
        this.component = component;
    }

    @Override
    public int depth() {
        if (this.component instanceof JArrayType aType) {
            return 1 + aType.depth();
        }
        return 1;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JArrayType at) {
            return JType.equals(this.component(), at.component(), kind, seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        seen = JType.concat(seen, this);

        return JType.hashCode(this.component(), seen);
    }

}
