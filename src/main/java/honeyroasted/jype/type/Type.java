package honeyroasted.jype.type;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.TypeVisitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
        seen = concat(seen, this);

        if (other instanceof MetaVarType mvt) {
            Set<Type> finalSeen = seen;
            return this.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.typeEquals(other, finalSeen));
        } else if (other instanceof IntersectionType it) {
            return this.equals(it, seen) || (it.children().size() == 1 && this.typeEquals(it.children().iterator().next(), seen));
        }
        return this.equals(other, seen);
    }

    static <T> Set<T> concat(Set<T> set, T... vals) {
        Set<T> newSet = new HashSet<>(set);
        Collections.addAll(newSet, vals);
        return newSet;
    }

    static boolean equals(Type left, Type right, Set<Type> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, seen);
    }

    static int hashCode(Type type, Set<Type> seen) {
        if (type == null) return 0;
        return type.hashCode(seen);
    }

    static boolean equals(List<? extends Type> left, List<? extends Type> right, Set<Type> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (int i = 0; i < left.size(); i++) {
                if (!left.get(i).equals(right.get(i), seen)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static int hashCode(List<? extends Type> list, Set<Type> seen) {
        if (list == null) return 0;

        int hash = 1;
        for (Type t : list) {
            hash = (hash * 31) + Type.hashCode(t, seen);
        }
        return hash;
    }

    static boolean equals(Set<? extends Type> left, Set<? extends Type> right, Set<Type> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (Type lt : left) {
                boolean contains = false;
                for (Type rt : right) {
                    if (lt.equals(rt, seen)) {
                        contains = true;
                        break;
                    }
                }

                if (!contains) return false;
            }

            return true;
        }
        return false;
    }

    static int hashCode(Set<? extends Type> set, Set<Type> seen) {
        if (seen == null) return 0;

        int hash = 1;
        for (Type t : seen) {
            hash += Type.hashCode(t, seen);
        }
        return hash;
    }

    static int multiHash(int... hashes) {
        int hash = 1;
        for (int i : hashes) {
            hash = hash * 31 + i;
        }
        return hash;
    }

    boolean equals(Type other, Set<Type> seen);

    int hashCode(Set<Type> seen);

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
