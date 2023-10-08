package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class VarTypeImpl extends AbstractPossiblyUnmodifiableType implements VarType {
    private TypeParameterLocation location;
    private Set<Type> upperBounds = new LinkedHashSet<>();

    public VarTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBounds = WildType.linkedCopyOf(this.upperBounds);
    }

    @Override
    protected void makeModifiable() {
        this.upperBounds = new LinkedHashSet<>(this.upperBounds);
    }

    @Override
    public TypeParameterLocation location() {
        return this.location;
    }

    @Override
    public void setLocation(TypeParameterLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public Set<Type> upperBounds() {
        return this.upperBounds;
    }

    @Override
    public void setUpperBounds(Set<Type> upperBounds) {
        super.checkUnmodifiable();
        this.upperBounds = upperBounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof VarType)) return false;
        VarType varType = (VarType) o;
        return Objects.equals(location, varType.location());
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

}