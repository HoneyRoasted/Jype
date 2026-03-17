package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JReduceLambdaExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.Lambda
                && constraint.right() instanceof JClassType;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Lambda lambda = (JExpressionInformation.Lambda) constraint.left();
        JClassType targetType = (JClassType) constraint.right();

        // JLS §18.2.1: T must be a functional interface type
        if (!targetType.classReference().hasModifier(AccessFlag.INTERFACE)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        // Find the Single Abstract Method - JLS §9.8
        Optional<JMethodReference> samOpt = findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();

        // Resolve type variables through the target type's parameterization
        if (targetType instanceof JParameterizedClassType pct) {
            sam = (JMethodReference) pct.varTypeResolver().visit(sam);
        }

        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(targetType.typeSystem().constants().voidType());

        // JLS §18.2.1: lambda parameter count must match SAM parameter count
        if (lambda.parameterCount() != samParams.size()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        // JLS §18.2.1: implicitly typed lambda requires all SAM param types to be proper types
        boolean explicitlyTyped = lambda.explicitParameterTypes() != null
                && lambda.explicitParameterTypes().size() == lambda.parameterCount();
        if (!explicitlyTyped) {
            for (JType p : samParams) {
                if (!p.isProperType()) {
                    branch.set(constraint, Constraint.Status.FALSE);
                    return;
                }
            }
        }

        // Reduce to sub-constraints
        branch.drop(constraint);

        // JLS §18.2.1: explicit parameter type equality constraints ‹Fi = Gi›
        if (explicitlyTyped) {
            for (int i = 0; i < lambda.parameterCount(); i++) {
                branch.add(new JTypeConstraints.Equal(lambda.explicitParameterTypes().get(i), samParams.get(i)));
            }
        }

        // JLS §18.2.1: return type constraint ‹body → R›
        if (!returnsVoid) {
            branch.add(new JTypeConstraints.ExpressionCompatible(
                    lambda.body(),
                    JTypeConstraints.Compatible.Context.ASSIGNMENT,
                    samReturn));
        }
    }

    /**
     * Finds the Single Abstract Method (SAM) of the given interface type per JLS §9.8.
     * Returns empty if the type is not a functional interface.
     */
    static Optional<JMethodReference> findFunctionalMethod(JClassType interfaceType) {
        JClassReference ref = interfaceType.classReference();
        if (!ref.hasModifier(AccessFlag.INTERFACE)) return Optional.empty();

        List<JMethodReference> allMethods = collectInterfaceMethods(ref, new LinkedHashSet<>());

        List<JMethodReference> abstractMethods = new ArrayList<>();
        for (JMethodReference m : allMethods) {
            if (m.hasModifier(AccessFlag.ABSTRACT)
                    && !m.hasModifier(AccessFlag.STATIC)
                    && !isObjectPublicMethod(m)) {
                abstractMethods.add(m);
            }
        }

        return abstractMethods.size() == 1 ? Optional.of(abstractMethods.get(0)) : Optional.empty();
    }

    /**
     * Collects all non-static non-private methods from the interface hierarchy,
     * deduplicating by override-equivalence so the most specific declaration wins.
     */
    private static List<JMethodReference> collectInterfaceMethods(JClassReference ref, Set<JClassReference> visited) {
        if (!visited.add(ref)) return Collections.emptyList();

        List<JMethodReference> result = new ArrayList<>();

        for (JMethodReference m : ref.declaredMethods()) {
            if (!m.hasModifier(AccessFlag.STATIC) && !m.hasModifier(AccessFlag.PRIVATE)) {
                if (result.stream().noneMatch(e -> isOverrideEquivalent(m, e))) {
                    result.add(m);
                }
            }
        }

        for (JClassType iface : ref.interfaces()) {
            for (JMethodReference m : collectInterfaceMethods(iface.classReference(), visited)) {
                if (result.stream().noneMatch(e -> isOverrideEquivalent(m, e))) {
                    result.add(m);
                }
            }
        }

        return result;
    }

    /**
     * Two methods are override-equivalent when they share the same name and erased parameter types (JLS §8.4.2).
     */
    static boolean isOverrideEquivalent(JMethodReference a, JMethodReference b) {
        if (!a.location().name().equals(b.location().name())) return false;
        if (a.parameters().size() != b.parameters().size()) return false;
        JMethodReference ea = (JMethodReference) JTypeVisitors.ERASURE.visit(a);
        JMethodReference eb = (JMethodReference) JTypeVisitors.ERASURE.visit(b);
        for (int i = 0; i < ea.parameters().size(); i++) {
            if (!ea.parameters().get(i).typeEquals(eb.parameters().get(i))) return false;
        }
        return true;
    }

    /**
     * Returns true when this method's signature matches a public instance method of Object.
     * Such methods are excluded from the SAM count per JLS §9.8.
     */
    private static boolean isObjectPublicMethod(JMethodReference m) {
        String name = m.location().name();
        int p = m.parameters().size();
        return switch (name) {
            case "equals" -> p == 1;
            case "hashCode", "toString", "getClass", "notify", "notifyAll" -> p == 0;
            case "wait" -> p == 0 || p == 1 || p == 2;
            default -> false;
        };
    }
}
