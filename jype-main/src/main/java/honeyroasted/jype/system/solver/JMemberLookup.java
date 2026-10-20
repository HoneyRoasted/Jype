package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JMemberLookup {

    private JMemberLookup() {
    }

    public static Set<JClassType> findClassTypes(JType type) {
        if (type instanceof JClassType ct) {
            return Set.of(ct);
        } else {
            Set<JClassType> result = new LinkedHashSet<>();
            Set<JType> visited = new LinkedHashSet<>();
            visited.add(type);
            Set<JType> frontier = Set.of(type);

            while (!frontier.isEmpty()) {
                Set<JType> next = new LinkedHashSet<>();

                for (JType curr : frontier) {
                    for (JType supertype : curr.knownDirectSupertypes()) {
                        if (supertype instanceof JClassType ct) {
                            result.add(ct);
                        } else if (visited.add(supertype)) {
                            next.add(supertype);
                        }
                    }
                }

                frontier = next;
            }

            return result;
        }
    }

    /**
     * An exception type is unchecked when it is a subtype of RuntimeException or Error (JLS §11.1.1);
     * unchecked exceptions are never restricted by a throws clause.
     */
    public static boolean isUncheckedException(JType type) {
        JTypeSystem system = type.typeSystem();
        return system.operations().isSubtype(type, system.constants().runtimeException())
                || system.operations().isSubtype(type, system.constants().error());
    }

    /**
     * The subset of a method's declared throws-clause types that are checked exceptions (JLS §11.2.2).
     */
    public static List<JType> checkedExceptions(JMethodReference ref) {
        return ref.exceptionTypes().stream().filter(t -> !isUncheckedException(t)).toList();
    }

    /**
     * JLS §18.2.6/§8.4.6: every checked exception a candidate method/constructor may throw must
     * be covered by the target functional method's throws clause ({@code samThrows}). If that
     * throws clause mentions an uninstantiated inference variable, the requirement is deferred
     * as a subtype bound on that variable (which defaults to RuntimeException per JLS §18.5.2 if
     * nothing else constrains it) rather than decided outright. Mutates {@code constraints} with
     * any deferred bounds and returns {@code false} if the candidate can be rejected outright.
     */
    public static boolean addThrowsConstraints(Map<Constraint, Constraint.Status> constraints, JMethodReference candidate, List<JType> samThrows) {
        List<JType> checked = checkedExceptions(candidate);
        if (checked.isEmpty()) return true;

        List<JType> properThrows = samThrows.stream().filter(JType::isProperType).toList();
        List<JMetaVarType> metavarThrows = samThrows.stream().filter(t -> t instanceof JMetaVarType).map(t -> (JMetaVarType) t).toList();

        for (JType exc : checked) {
            boolean directlyCovered = properThrows.stream().anyMatch(t -> exc.typeEquals(t) || exc.typeSystem().operations().isSubtype(exc, t));
            if (directlyCovered) continue;

            if (metavarThrows.isEmpty()) {
                return false;
            }

            for (JMetaVarType mvt : metavarThrows) {
                constraints.putIfAbsent(new JTypeConstraints.Throws(mvt), Constraint.Status.TRUE);
                constraints.put(new JTypeConstraints.Subtype(exc, mvt), Constraint.Status.UNKNOWN);
            }
        }
        return true;
    }

    /**
     * JLS §5.1.10: applies capture conversion to a member-access receiver type before it is used
     * for member lookup or generic substitution. If {@code type} is a parameterized class type
     * with at least one wildcard type argument, this creates a fresh metavariable per type
     * parameter position and records a {@code Capture} bound (consumed by
     * {@link honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationCapture})
     * describing how each fresh variable relates back to the original argument. Types with no
     * wildcard arguments are returned unchanged.
     */
    public static JType captureConvert(ConstraintBranch branch, JType type) {
        if (type instanceof JParameterizedClassType pct && pct.hasAnyTypeArguments()
                && pct.typeArguments().stream().anyMatch(t -> t instanceof JWildType)) {
            List<JArgumentType> fresh = pct.classReference().typeParameters().stream()
                    .<JArgumentType>map(vt -> vt.createMetaVar())
                    .toList();
            JParameterizedClassType captured = pct.classReference().parameterized(fresh);
            branch.add(new JTypeConstraints.Capture(captured, pct), Constraint.Status.TRUE);
            return captured;
        }
        return type;
    }

    public static Collection<JMethodReference> staticMethods(JClassReference ref) {
        List<JMethodReference> result = new ArrayList<>();

        Set<JClassType> building = Set.of(ref);
        Set<JClassType> next = new LinkedHashSet<>();

        while (!building.isEmpty()) {
            for (JClassType curr : building) {
                curr.classReference().declaredMethods()
                        .stream().filter(mref -> mref.hasModifier(AccessFlag.STATIC))
                        .forEach(result::add);

                if (curr.superClass() != null) next.add(curr.superClass());
                next.addAll(curr.interfaces());
            }

            building = next;
            next = new LinkedHashSet<>();
        }

        return result;
    }

    public static Collection<JMethodReference> allMethods(JClassReference ref) {
        List<JMethodReference> result = new ArrayList<>();

        Set<JClassType> building = Set.of(ref);
        Set<JClassType> next = new LinkedHashSet<>();

        while (!building.isEmpty()) {
            for (JClassType curr : building) {
                curr.classReference().declaredMethods()
                        .stream().filter(mref -> result.stream().noneMatch(currRef -> isOverriddenBy(mref, currRef)))
                        .forEach(result::add);

                if (curr.superClass() != null) next.add(curr.superClass());
                next.addAll(curr.interfaces());
            }

            building = next;
            next = new LinkedHashSet<>();
        }

        return result;
    }

    private static boolean isOverriddenBy(JMethodReference left, JMethodReference right) {
        left = (JMethodReference) JTypeVisitors.ERASURE.visit(left);
        right = (JMethodReference) JTypeVisitors.ERASURE.visit(right);

        JMethodReference bridge = findBridgeMethod(left);
        if (bridge != null) {
            return isOverriddenBy(bridge, right);
        }

        return left.location().name().equals(right.location().name()) &&
                isOverridableIn(left, right.outerClass()) &&
                !JAccess.fromFlags(right.modifiers()).isMoreRestrictiveThan(JAccess.fromFlags(left.modifiers())) &&
                left.returnType().typeEquals(right.returnType()) &&
                areParametersEqual(left, right);
    }

    private static JMethodReference findBridgeMethod(JMethodReference me) {
        if (isBridge(me.modifiers())) return null;
        return me.outerClass().declaredMethods().stream()
                .map(it -> (JMethodReference) JTypeVisitors.ERASURE.visit(it))
                .filter(it -> !it.equals(me) && isBridge(it.modifiers()) && it.location().name().equals(me.location().name()) && areParametersCovariant(me, it) && it.returnType().isAssignableFrom(me.returnType()))
                .findFirst().orElse(null);
    }

    private static boolean isOverridableIn(JMethodReference me, JClassType cls) {
        if (!isOverridable(me.modifiers())) return false;
        if (!isSubclassVisible(me.modifiers())) return false;
        if (!me.outerClass().isAssignableFrom(cls)) return false;

        if (Modifier.isPublic(me.modifiers())) return true;
        return isPackageVisible(me.modifiers()) && Objects.equals(cls.namespace().location().getPackage(), me.outerClass().namespace().location().getPackage());
    }

    private static boolean areParametersCovariant(JMethodReference left, JMethodReference right) {
        List<JType> leftParams = left.parameters();
        List<JType> rightParams = right.parameters();

        if (leftParams.size() != rightParams.size()) return false;

        for (int i = 0; i < leftParams.size(); i++) {
            if (!rightParams.get(i).isAssignableFrom(leftParams.get(i))) return false;
        }

        return true;
    }

    private static boolean areParametersEqual(JMethodReference left, JMethodReference right) {
        List<JType> leftParams = left.parameters();
        List<JType> rightParams = right.parameters();

        if (leftParams.size() != rightParams.size()) return false;

        for (int i = 0; i < leftParams.size(); i++) {
            if (!rightParams.get(i).typeEquals(leftParams.get(i))) return false;
        }

        return true;
    }

    private static boolean isOverridable(int mods) {
        return !Modifier.isStatic(mods) &&
                !Modifier.isFinal(mods) &&
                !Modifier.isPrivate(mods);
    }

    private static boolean isBridge(int mods) {
        return (mods & AccessFlag.BRIDGE.mask()) != 0;
    }

    private static boolean isPackageVisible(int mods) {
        return !Modifier.isPrivate(mods);
    }

    private static boolean isSubclassVisible(int mods) {
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

}
