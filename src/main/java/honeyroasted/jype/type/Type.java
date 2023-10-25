package honeyroasted.jype.type;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.TypeVisitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface Type extends Copyable<Type> {

    TypeSystem typeSystem();

    String simpleName();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

    default boolean typeEquals(Type other) {
        return this.typeEquals(other, new HashSet<>());
    }

    default boolean typeEquals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;
        seen.add(this);

        if (other instanceof MetaVarType mvt) {
            return this.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.typeEquals(other, seen));
        } else if (other instanceof IntersectionType it) {
            return this.equals(it) || (it.children().size() == 1 && this.typeEquals(it.children().iterator().next(), seen));
        }
        return this.equals(other);
    }

    default boolean isNullType() {
        return false;
    }

    default Set<Type> knownDirectSupertypes() {
        return Collections.emptySet();
    }

    default <T extends Type> T stripMetadata() {
        return (T) this;
    }

    <T extends Type> T copy(TypeCache<Type, Type> cache);

    default <T extends Type> T copy() {
        return copy(new InMemoryTypeCache<>());
    }

    default boolean isProperType() {
        return TypeVisitors.IS_PROPER_TYPE.visit(this);
    }

    default boolean hasCyclicTypeVariables() {
        return this.hasCyclicTypeVariables(new HashSet<>());
    }

    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return false;
    }

}
