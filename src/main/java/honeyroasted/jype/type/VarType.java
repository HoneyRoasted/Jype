package honeyroasted.jype.type;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Set;

public interface VarType extends PossiblyUnmodifiable, Type {
    TypeParameterLocation location();

    void setLocation(TypeParameterLocation location);

    Set<Type> upperBounds();

    void setUpperBounds(Set<Type> upperBounds);

    Set<Type> lowerBounds();

    void setLowerBounds(Set<Type> lowerBounds);

    @Override
    default TypeWithMetadata<VarType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitVarType(this, context);
    }
}
