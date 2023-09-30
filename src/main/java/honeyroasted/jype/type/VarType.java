package honeyroasted.jype.type;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VarType extends AbstractPossiblyUnmodifiableType {
    private TypeParameterLocation location;
    private List<Type> upperBounds = new ArrayList<>();
    private List<Type> lowerBounds = new ArrayList<>();

    public VarType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBounds = List.copyOf(this.upperBounds);
    }

    @Override
    protected void makeModifiable() {
        this.upperBounds = new ArrayList<>(this.upperBounds);
    }

    public TypeParameterLocation location() {
        return this.location;
    }

    public void setLocation(TypeParameterLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    public List<Type> upperBounds() {
        return this.upperBounds;
    }

    public void setUpperBounds(List<Type> upperBounds) {
        super.checkUnmodifiable();
        this.upperBounds = upperBounds;
    }

    public List<Type> lowerBounds() {
        return this.lowerBounds;
    }

    public void setLowerBounds(List<Type> lowerBounds) {
        super.checkUnmodifiable();
        this.lowerBounds = lowerBounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarType varType = (VarType) o;
        return Objects.equals(location, varType.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitTypeVar(this, context);
    }
}
