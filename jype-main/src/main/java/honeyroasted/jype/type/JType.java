package honeyroasted.jype.type;

import honeyroasted.almonds.SimpleName;
import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JInMemoryTypeCache;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JDownwardProjectionVisitor;
import honeyroasted.jype.system.visitor.visitors.JToSignatureVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface JType extends SimpleName, Copyable<JType, JTypeCache<JType, JType>> {

    JTypeSystem typeSystem();

    default JSignature signature() {
        return JTypeVisitors.TO_SIGNATURE.visit(this, JToSignatureVisitor.Mode.USAGE);
    }

    default JSignature declarationSignature() {
        return JTypeVisitors.TO_SIGNATURE.visit(this, JToSignatureVisitor.Mode.DECLARATION);
    }

    default JDescriptor descriptor() {
        return JTypeVisitors.TO_DESCRIPTOR.visit(this);
    }

    <R, P> R accept(JTypeVisitor<R, P> visitor, P context);

    PropertySet metadata();

    default JVarTypeResolveVisitor varTypeResolver() {
        return new JVarTypeResolveVisitor(Collections.emptyMap());
    }

    default JType upwardsProjection() {
        return upwardsProjection(t -> t instanceof JMetaVarType);
    }

    default JType upwardsProjection(Predicate<JType> restricted) {
        return accept(JTypeVisitors.upwardProjection(restricted));
    }

    default Optional<JType> downwardsProjection() {
        return downwardsProjection(t -> t instanceof JMetaVarType);
    }

    default Optional<JType> downwardsProjection(Predicate<JType> restricted) {
        try {
            return Optional.of(accept(JTypeVisitors.downwardProjection(restricted)));
        } catch (JDownwardProjectionVisitor.UndefinedProjectionException ex) {
            return Optional.empty();
        }
    }

    default <R, P> R accept(JTypeVisitor<R, P> visitor) {
        return accept(visitor, null);
    }

    default boolean accept(Predicate<JType> visitor) {
        return accept(JTypeVisitors.typePredicate(visitor));
    }

    default boolean isCompatibleTo(JType other, JTypeConstraints.Compatible.Context context) {
        return this.typeSystem().operations().isCompatible(this, other, context);
    }

    default boolean isCompatibleFrom(JType other, JTypeConstraints.Compatible.Context context) {
        return this.typeSystem().operations().isCompatible(other, this, context);
    }

    default boolean isAssignableTo(JType other) {
        return this.isCompatibleTo(other, JTypeConstraints.Compatible.Context.ASSIGNMENT);
    }

    default boolean isAssignableFrom(JType other) {
        return this.isCompatibleFrom(other, JTypeConstraints.Compatible.Context.ASSIGNMENT);
    }

    static boolean baseCaseEquivalence(JType left, JType other, Set<Pair<JType, JType>> seen) {
        if (left == other) return true;
        if (seen.contains(Pair.identity(left, other))) return true;

        if (left instanceof JMetaVarType mvt) {
            Set<Pair<JType, JType>> finalSeen = concat(seen, Pair.identity(other, left));
            return other.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.equals(left, Equality.EQUIVALENT, finalSeen));
        } else if (left instanceof JIntersectionType it) {
            seen = concat(seen, Pair.identity(other, left));
            return other.equals(it, Equality.STRUCTURAL, seen) || (it.children().size() == 1 && other.equals(it.children().iterator().next(), Equality.EQUIVALENT, seen));
        }

        if (other instanceof JMetaVarType mvt) {
            Set<Pair<JType, JType>> finalSeen = concat(seen, Pair.identity(left, other));
            return left.equals(mvt) || mvt.equalities().stream().anyMatch(t -> t.equals(other, Equality.EQUIVALENT, finalSeen));
        } else if (other instanceof JIntersectionType it) {
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

    static boolean typeEquals(JType left, JType right) {
        return typeEquals(left, right, new HashSet<>());
    }

    static boolean typeEquals(JType left, JType right, Set<Pair<JType, JType>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, Equality.EQUIVALENT, seen);
    }

    static boolean structuralEquals(JType left, JType right) {
        return structuralEquals(left, right, new HashSet<>());
    }

    static boolean structuralEquals(JType left, JType right, Set<Pair<JType, JType>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, Equality.STRUCTURAL, seen);
    }

    static boolean equals(JType left, JType right, Equality kind, Set<Pair<JType, JType>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return left.equals(right, kind, seen);
    }


    static boolean equals(List<? extends JType> left, List<? extends JType> right, Equality kind, Set<Pair<JType, JType>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (int i = 0; i < left.size(); i++) {
                if (!JType.equals(left.get(i), right.get(i), kind, seen)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    static boolean equals(Set<? extends JType> left, Set<? extends JType> right, Equality kind, Set<Pair<JType, JType>> seen) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left.size() == right.size()) {
            for (JType lt : left) {
                boolean contains = false;
                for (JType rt : right) {
                    if (JType.equals(lt, rt, kind, seen)) {
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

    static int hashCode(JType type, Set<JType> seen) {
        if (type == null) return 0;
        return type.hashCode(seen);
    }

    static int hashCode(List<? extends JType> list, Set<JType> seen) {
        if (list == null) return 0;

        int hash = 1;
        for (JType t : list) {
            hash = (hash * 31) + JType.hashCode(t, seen);
        }
        return hash;
    }

    static int hashCode(Set<? extends JType> set, Set<JType> seen) {
        if (set == null) return 0;

        int hash = 1;
        for (JType t : set) {
            hash += JType.hashCode(t, seen);
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

    boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen);

    default boolean equals(JType other, Equality kind) {
        return this.equals(other, kind, new HashSet<>());
    }

    default boolean typeEquals(JType other) {
        return this.equals(other, Equality.EQUIVALENT);
    }

    default boolean structuralEquals(JType other) {
        return this.equals(other, Equality.STRUCTURAL);
    }

    enum Equality {
        STRUCTURAL,
        EQUIVALENT
    }

    int hashCode(Set<JType> seen);

    default boolean isNullType() {
        return false;
    }

    default Set<JType> knownDirectSupertypes() {
        return Collections.emptySet();
    }

    <T extends JType> T copy(JTypeCache<JType, JType> cache);

    @Override
    default <T extends JType> T copy() {
        return copy(new JInMemoryTypeCache<>(JType.class, JType.class));
    }

    default boolean isProperType() {
        return JTypeVisitors.IS_PROPER_TYPE.visit(this);
    }

    default boolean hasCyclicTypeVariables() {
        return this.hasCyclicTypeVariables(new HashSet<>());
    }

    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return false;
    }

    @Override
    default boolean isResultType(Object object) {
        return object instanceof JType;
    }

    @Override
    default boolean isContextType(Object object) {
        return object instanceof JTypeCache<?, ?> tc && tc.keyType() == JType.class && tc.valueType() == JType.class;
    }
}
