package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.HashSet;
import java.util.Set;

public interface Type {

    TypeSystem typeSystem();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

    default Type stripMetadata() {
        return this;
    }

    default boolean hasCyclicTypeVariables() {
        return this.hasCyclicTypeVariables(new HashSet<>());
    }

    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return false;
    }

}
