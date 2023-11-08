package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class IntersectionTypeImpl extends AbstractPossiblyUnmodifiableType implements IntersectionType {
    private Set<Type> children;

    public IntersectionTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.children = Collections.unmodifiableSet(new LinkedHashSet<>(this.children));
    }

    @Override
    protected void makeModifiable() {
        this.children = new LinkedHashSet<>(this.children);
    }

    @Override
    public Set<Type> children() {
        return this.children;
    }

    @Override
    public void setChildren(Set<Type> children) {
        if (children.stream().anyMatch(t -> t instanceof IntersectionType)) {
            throw new IllegalArgumentException("Intersection type may not be nested");
        }

        this.children = children;
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        IntersectionType copy = new IntersectionTypeImpl(this.typeSystem());
        cache.put(this, copy);
        copy.setChildren((Set) this.children.stream().map(Type::copy).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public boolean equals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;
        seen = Type.concat(seen, this);

        if (other instanceof IntersectionType it) {
            return Type.equals(children, it.children(), seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.hashCode(children, seen);
    }

}
