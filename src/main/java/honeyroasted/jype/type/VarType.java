package honeyroasted.jype.type;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.HashSet;
import java.util.Set;

public interface VarType extends PossiblyUnmodifiable, Type {
    TypeParameterLocation location();

    void setLocation(TypeParameterLocation location);

    Set<Type> upperBounds();

    void setUpperBounds(Set<Type> upperBounds);

    Set<Type> lowerBounds();

    void setLowerBounds(Set<Type> lowerBounds);

    @Override
    default VarType stripMetadata() {
        return this;
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        if (seen.contains(this)) return true;
        seen.add(this);
        return this.upperBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)))
                || this.lowerBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitVarType(this, context);
    }
}
