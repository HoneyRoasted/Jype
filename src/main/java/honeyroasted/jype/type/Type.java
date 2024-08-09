package honeyroasted.jype.type;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.solver._old.solvers.CompatibilityTypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.TypeVisitors;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface Type extends Copyable<Type> {

    TypeSystem typeSystem();

    String simpleName();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

    default <R, P> R accept(TypeVisitor<R, P> visitor) {
        return accept(visitor, null);
    }

    default boolean isCompatibleTo(Type other, TypeBound.Compatible.Context context) {
        return new CompatibilityTypeSolver()
                .bind(new TypeBound.Compatible(this, other, context))
                .solve(this.typeSystem())
                .success();
    }

    default boolean isAssignableTo(Type other) {
        return this.isCompatibleTo(other, TypeBound.Compatible.Context.ASSIGNMENT);
    }
    
    static boolean baseCaseEquivalence(Type left, Type other, Set<Pair<Type, Type>> seen) {
        if (left == other) return true;
        if (seen.contains(Pair.identity(left, other))) return true;

        if (left instanceof MetaVarType mvt) {
            Set<Pair<Type, Type>> finalSeen = concat(seen, Pair.identity(other, left));
            return other.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.equals(left, Equality.EQUIVALENT, finalSeen));
        } else if (left instanceof IntersectionType it) {
            seen = concat(seen, Pair.identity(other, left));
            return other.equals(it, Equality.STRUCTURAL, seen) || (it.children().size() == 1 && other.equals(it.children().iterator().next(), Equality.EQUIVALENT, seen));
        }
        
        if (other instanceof MetaVarType mvt) {
            Set<Pair<Type, Type>> finalSeen = concat(seen, Pair.identity(left, other));
            return left.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.equals(other, Equality.EQUIVALENT, finalSeen));
        } else if (other instanceof IntersectionType it) {
            seen = concat(seen, Pair.identity(left, other));
            return left.equals(it, Equality.STRUCTURAL, seen) || (it.children().size() == 1 && left.equals(it.children().iterator().next(), Equality.EQUIVALENT, seen));
        }
        
        
        return false;
    }

    static <T> Set<T> concat(Set<T> set, T... vals) {
        Set<T> newSet;

        if (set instanceof LinkedHashSet<T>) {
            newSet = new LinkedHashSet<>(set);
        } else if (set instanceof HashSet<T>) {
            newSet = new HashSet<>(set);
        } else {
            newSet = Collections.newSetFromMap(new IdentityHashMap<>());
            newSet.addAll(set);
        }

        Collections.addAll(newSet, vals);
        return newSet;
    }

    static boolean typeEquals(Type left, Type right) {
        return typeEquals(left, right, new HashSet<>());
    }

    static boolean typeEquals(Type left, Type right, Set<Pair<Type, Type>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, Equality.EQUIVALENT, seen);
    }

    static boolean structuralEquals(Type left, Type right) {
        return structuralEquals(left, right, new HashSet<>());
    }

    static boolean structuralEquals(Type left, Type right, Set<Pair<Type, Type>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, Equality.STRUCTURAL, seen);
    }
    
    static boolean equals(Type left, Type right, Equality kind, Set<Pair<Type, Type>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, kind, seen);    
    }
    

    static boolean equals(List<? extends Type> left, List<? extends Type> right, Equality kind, Set<Pair<Type, Type>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (int i = 0; i < left.size(); i++) {
                if (!Type.equals(left.get(i), right.get(i), kind, seen)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static boolean equals(Set<? extends Type> left, Set<? extends Type> right, Equality kind, Set<Pair<Type, Type>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (Type lt : left) {
                boolean contains = false;
                for (Type rt : right) {
                    if (Type.equals(lt, rt, kind, seen)) {
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

    static int hashCode(Type type, Set<Type> seen) {
        if (type == null) return 0;
        return type.hashCode(seen);
    }
    
    static int hashCode(List<? extends Type> list, Set<Type> seen) {
        if (list == null) return 0;

        int hash = 1;
        for (Type t : list) {
            hash = (hash * 31) + Type.hashCode(t, seen);
        }
        return hash;
    }

    static int hashCode(Set<? extends Type> set, Set<Type> seen) {
        if (set == null) return 0;

        int hash = 1;
        for (Type t : set) {
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

    boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen);

    default boolean equals(Type other, Equality kind) {
        return this.equals(other, kind, new HashSet<>());
    }

    default boolean typeEquals(Type other) {
        return this.equals(other, Equality.EQUIVALENT);
    }

    default boolean structuralEquals(Type other) {
        return this.equals(other, Equality.STRUCTURAL);
    }
    
    enum Equality {
        STRUCTURAL,
        EQUIVALENT
    }

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
