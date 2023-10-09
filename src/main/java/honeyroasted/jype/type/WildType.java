package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.*;

public interface WildType extends PossiblyUnmodifiable, Type {

    int identity();

    void setIdentity(int identity);

    Set<Type> upperBounds();

    void setUpperBounds(Set<Type> upperBounds);

    Set<Type> lowerBounds();

    void setLowerBounds(Set<Type> lowerBounds);

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitWildcardType(this, context);
    }

    static <T> Set<T> linkedCopyOf(Collection<T> set) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(set));
    }

    interface Upper extends WildType {
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
