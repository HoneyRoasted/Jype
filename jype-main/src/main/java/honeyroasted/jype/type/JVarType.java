package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.HashSet;
import java.util.Set;

public interface JVarType extends PossiblyUnmodifiable, JType, JArgumentType {
    JTypeParameterLocation location();

    default String name() {
        return this.location().name();
    }

    void setLocation(JTypeParameterLocation location);

    Set<JType> upperBounds();

    void setUpperBounds(Set<JType> upperBounds);

    default JType upperBound() {
        return JIntersectionType.of(upperBounds(), typeSystem());
    }

    default boolean hasDefaultBounds() {
        return this.upperBounds().isEmpty() || this.upperBounds().equals(Set.of(this.typeSystem().constants().object()));
    }

    default JMetaVarType createMetaVar() {
        return this.typeSystem().typeFactory().newMetaVarType(System.identityHashCode(this), this.name());
    }

    @Override
    default Set<JType> knownDirectSupertypes() {
        return this.upperBounds();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        if (seen.contains(this)) return true;
        seen.add(this);
        return this.upperBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitVarType(this, context);
    }
}
