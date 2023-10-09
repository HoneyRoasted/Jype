package honeyroasted.jype.type;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.HashSet;
import java.util.Set;

public interface Type extends Copyable<Type> {

    TypeSystem typeSystem();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

    default <T extends Type> T stripMetadata() {
        return (T) this;
    }

    <T extends Type> T copy(TypeCache<Type, Type> cache);

    default <T extends Type> T copy() {
        return copy(new InMemoryTypeCache<>());
    }

    default boolean hasCyclicTypeVariables() {
        return this.hasCyclicTypeVariables(new HashSet<>());
    }

    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return false;
    }

}
