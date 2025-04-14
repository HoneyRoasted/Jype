package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public interface JWildType extends PossiblyUnmodifiable, JType, JArgumentType {

    int identity();

    void setIdentity(int identity);

    Set<JType> upperBounds();

    default JType upperBound() {
        if (this.upperBounds().isEmpty()) {
            return this.typeSystem().constants().object();
        } else if (this.upperBounds().size() == 1) {
            return this.upperBounds().iterator().next();
        } else {
            return JIntersectionType.of(upperBounds(), this.typeSystem());
        }
    }

    default JType lowerBound() {
        if (this.lowerBounds().isEmpty()) {
            return this.typeSystem().constants().nullType();
        } else if (this.lowerBounds().size() == 1) {
            return this.lowerBounds().iterator().next();
        } else {
            return JIntersectionType.of(lowerBounds(), this.typeSystem());
        }
    }

    void setUpperBounds(Set<JType> upperBounds);

    Set<JType> lowerBounds();

    void setLowerBounds(Set<JType> lowerBounds);

    @Override
    default Set<JType> knownDirectSupertypes() {
        return this.upperBounds();
    }

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitWildcardType(this, context);
    }

    static <T> Set<T> linkedCopyOf(Collection<T> set) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(set));
    }

    interface Upper extends JWildType {

        default boolean hasDefaultBounds() {
            return this.upperBounds().isEmpty() || this.upperBounds().equals(Set.of(this.typeSystem().constants().object()));
        }

        @Override
        default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
            return this.upperBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
        }

    }

    interface Lower extends JWildType {

        @Override
        default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
            return this.lowerBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
        }

    }

}
