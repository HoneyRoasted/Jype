package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JIntersectionTypeImpl extends JAbstractPossiblyUnmodifiableType implements JIntersectionType {
    private Set<JType> children;

    public JIntersectionTypeImpl(JTypeSystem typeSystem) {
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
    public Set<JType> children() {
        return this.children;
    }

    private boolean isRedundant(List<JType> list, int i) {
        JType current = list.get(i);
        for (int j = 0; j < list.size(); j++) {
            if (i != j) {
                JType other = list.get(j);
                if (other.typeEquals(current)) {
                    if (j < i) {
                        return true;
                    }
                } else if (this.typeSystem().operations().isSubtype(other, current)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSimplified() {
        List<JType> list = new ArrayList<>(this.children());
        for (int i = 0; i < list.size(); i++) {
            if (isRedundant(list, i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public JType simplify() {
        Set<JType> newChildren = new LinkedHashSet<>();
        Set<JType> currChildren = JIntersectionType.flatten(this.children);
        List<JType> list = new ArrayList<>(currChildren);

        for (int i = 0; i < list.size(); i++) {
            if (!isRedundant(list, i)) {
                newChildren.add(list.get(i));
            }
        }

        if (newChildren.isEmpty() && !currChildren.isEmpty()) {
            newChildren.add(currChildren.iterator().next());
        }

        if (newChildren.size() == 1) {
            return newChildren.iterator().next();
        }

        JIntersectionType type = this.typeSystem().typeFactory().newIntersectionType();
        type.setChildren(newChildren);
        type.setUnmodifiable(true);
        return type;
    }

    @Override
    public void setChildren(Set<JType> children) {
        if (children.stream().anyMatch(t -> t instanceof JIntersectionType)) {
            throw new IllegalArgumentException("Intersection type may not be nested");
        }

        this.children = children;
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) {
            return (T) cached.get();
        }

        JIntersectionType copy = this.typeSystem().typeFactory().newIntersectionType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setChildren((Set) this.children.stream().map(JType::copy).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) {
            return true;
        }
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) {
            return true;
        }
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JIntersectionType it) {
            if (this.isSimplified() && it.isSimplified()) {
                return JType.equals(children, it.children(), kind, seen);
            } else {
                return this.simplify().equals(it.simplify(), kind, seen);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) {
            return 0;
        }
        seen = JType.concat(seen, this);

        return JType.hashCode(children, seen);
    }

}
