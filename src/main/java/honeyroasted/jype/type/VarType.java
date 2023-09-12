package honeyroasted.jype.type;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VarType extends AbstractPossiblyUnmodifiableType {
    private TypeParameterLocation location;
    private List<Type> bounds = new ArrayList<>();

    public VarType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.bounds = List.copyOf(this.bounds);
    }

    @Override
    protected void makeModifiable() {
        this.bounds = new ArrayList<>(this.bounds);
    }

    public TypeParameterLocation location() {
        return this.location;
    }

    public void setLocation(TypeParameterLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }


    public List<Type> bounds() {
        return this.bounds;
    }

    public void setBounds(List<Type> bounds) {
        super.checkUnmodifiable();
        this.bounds = bounds;
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
}
