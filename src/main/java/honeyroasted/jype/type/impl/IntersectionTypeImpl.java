package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
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
    public String simpleName() {
        return this.children.stream().map(Type::simpleName).collect(Collectors.joining(" & "));
    }

    @Override
    public String toString() {
        return this.children.stream().map(Type::toString).collect(Collectors.joining(" & "));
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        IntersectionType copy = new IntersectionTypeImpl(this.typeSystem());
        copy.setChildren((Set) this.children.stream().map(Type::copy).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this.children.size() == 1) {
            return this.children.iterator().next().equals(o);
        }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntersectionTypeImpl that = (IntersectionTypeImpl) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        if (this.children.size() == 1) {
            return this.children.iterator().next().hashCode();
        }

        return Objects.hash(children);
    }
}
