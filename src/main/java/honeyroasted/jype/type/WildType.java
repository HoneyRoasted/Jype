package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public interface WildType extends PossiblyUnmodifiable, Type, ArgumentType {

    int identity();

    void setIdentity(int identity);

    Set<Type> upperBounds();

    default Type upperBound() {
        if (this.upperBounds().isEmpty()) {
            return this.typeSystem().constants().object();
        } else if (this.upperBounds().size() == 1) {
            return this.upperBounds().iterator().next();
        } else {
            IntersectionType type = new IntersectionTypeImpl(this.typeSystem());
            type.setChildren(IntersectionType.flatten(upperBounds()));
            type.setUnmodifiable(true);
            return type;
        }
    }

    default Type lowerBound() {
        if (this.lowerBounds().isEmpty()) {
            return this.typeSystem().constants().nullType();
        } else if (this.lowerBounds().size() == 1) {
            return this.lowerBounds().iterator().next();
        } else {
            IntersectionType type = new IntersectionTypeImpl(this.typeSystem());
            type.setChildren(IntersectionType.flatten(lowerBounds()));
            type.setUnmodifiable(true);
            return type;
        }
    }

    void setUpperBounds(Set<Type> upperBounds);

    Set<Type> lowerBounds();

    void setLowerBounds(Set<Type> lowerBounds);

    @Override
    default Set<Type> knownDirectSupertypes() {
        return this.upperBounds();
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitWildcardType(this, context);
    }

    static <T> Set<T> linkedCopyOf(Collection<T> set) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(set));
    }

    interface Upper extends WildType {

        default boolean hasDefaultBounds() {
            return this.upperBounds().isEmpty() || this.upperBounds().equals(Set.of(this.typeSystem().constants().object()));
        }

        @Override
        default WildType.Upper stripMetadata() {
            return this;
        }

        @Override
        default boolean hasCyclicTypeVariables(Set<VarType> seen) {
            return this.upperBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
        }

    }

    interface Lower extends WildType {
        @Override
        default WildType.Lower stripMetadata() {
            return this;
        }

        @Override
        default boolean hasCyclicTypeVariables(Set<VarType> seen) {
            return this.lowerBounds().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
        }

    }

}
