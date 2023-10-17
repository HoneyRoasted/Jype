package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WildTypeLowerImpl extends AbstractPossiblyUnmodifiableType implements WildType.Lower {
    private Set<Type> lowerBound = new LinkedHashSet<>();
    private int identity;

    public WildTypeLowerImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public String simpleName() {
        return "? super " + this.lowerBound.stream().map(Type::simpleName).collect(Collectors.joining(" & "));
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        WildType.Lower copy = new WildTypeLowerImpl(this.typeSystem());
        copy.setIdentity(this.identity);
        copy.setLowerBounds(this.lowerBound.stream().map(t -> (Type) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeModifiable() {
        this.lowerBound = new LinkedHashSet<>(this.lowerBound);
    }

    @Override
    protected void makeUnmodifiable() {
        this.lowerBound = WildType.linkedCopyOf(this.lowerBound);
    }

    @Override
    public int identity() {
        return this.identity;
    }

    @Override
    public void setIdentity(int identity) {
        this.checkUnmodifiable();
        this.identity = identity;
    }

    @Override
    public Set<Type> upperBounds() {
        return Set.of(this.typeSystem().constants().object());
    }

    @Override
    public void setUpperBounds(Set<Type> upperBounds) {

    }

    @Override
    public Set<Type> lowerBounds() {
        return this.lowerBound;
    }

    public void setLowerBounds(Set<Type> lowerBound) {
        super.checkUnmodifiable();
        this.lowerBound = lowerBound;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntersectionType it) {
            return it.equals(this);
        }

        return o != null && o instanceof WildType wt && wt.identity() == this.identity;
    }

    @Override
    public int hashCode() {
        return this.identity;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("? super ");
        Iterator<Type> iterator = this.lowerBound.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(" & ");
            }
        }
        return sb.toString();
    }
}
