package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.Pair;
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
    public boolean isSimplified() {
        for (Type current : this.children()) {
            for (Type other : this.children()) {
                if (current != other && this.typeSystem().operations().isSubtype(other, current)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public Type simplify() {
        Set<Type> newChildren = new LinkedHashSet<>();
        for (Type current : this.children()) {
            boolean foundSubtype = false;
            for (Type other : this.children()) {
                if (current != other && this.typeSystem().operations().isSubtype(other, current)) {
                    foundSubtype = true;
                    break;
                }
            }

            if (!foundSubtype) {
                newChildren.add(current);
            }
        }

        if (newChildren.isEmpty() && !this.children().isEmpty()) {
            newChildren.add(this.children().iterator().next());
        }

        if (newChildren.size() == 1) {
            return newChildren.iterator().next();
        }

        IntersectionType type = this.typeSystem().typeFactory().newIntersectionType();
        type.setChildren(newChildren);
        type.setUnmodifiable(true);
        return type;
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

        IntersectionType copy = this.typeSystem().typeFactory().newIntersectionType();
        cache.put(this, copy);
        copy.setChildren((Set) this.children.stream().map(Type::copy).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;
        seen = Type.concat(seen, Pair.identity(this, other));

        if (other instanceof IntersectionType it) {
            if (this.isSimplified() && it.isSimplified()) {
                return Type.equals(children, it.children(), kind, seen);
            } else {
                return this.simplify().equals(it.simplify(), kind, seen);
            }
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
