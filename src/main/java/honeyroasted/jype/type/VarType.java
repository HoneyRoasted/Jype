package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.HashSet;
import java.util.Set;

public interface VarType extends PossiblyUnmodifiable, Type, ArgumentType {
    TypeParameterLocation location();

    default String name() {
        return this.location().name();
    }

    void setLocation(TypeParameterLocation location);

    Set<Type> upperBounds();

    void setUpperBounds(Set<Type> upperBounds);

    default boolean hasDefaultBounds() {
        return this.upperBounds().isEmpty() || this.upperBounds().equals(Set.of(this.typeSystem().constants().object()));
    }

    default MetaVarType createMetaVar() {
        return this.typeSystem().typeFactory().newMetaVarType(System.identityHashCode(this), this.name());
    }

    @Override
    default Set<Type> knownDirectSupertypes() {
        return this.upperBounds();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        if (seen.contains(this)) return true;
        seen.add(this);
        return this.upperBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitVarType(this, context);
    }
}
