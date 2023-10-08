package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class WildTypeLowerImpl extends AbstractPossiblyUnmodifiableType implements WildType.Lower {
    private Set<Type> lowerBound = new LinkedHashSet<>();
    private int identity;

    public WildTypeLowerImpl(TypeSystem typeSystem) {
        super(typeSystem);
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