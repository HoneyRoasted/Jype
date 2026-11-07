package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JOperatorTypes;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.List;

public class JReduceConditionalExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Conditional && !honeyroasted.jype.system.solver.constraints.compatibility.JExpressionAssignmentConstant.applies(constraint);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Conditional cond = (JExpressionInformation.Conditional) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JExpressionInformation whenTrue = cond.whenTrue();
        JExpressionInformation whenFalse = cond.whenFalse();
        boolean polyContext = constraint.middle().isPoly();

        // JLS §15.25 the first operand must be boolean or Boolean
        Constraint conditionCheck = new JTypeConstraints.ExpressionCompatible(cond.condition(), JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, system.constants().booleanType());

        if (isInherentlyPoly(whenTrue) || isInherentlyPoly(whenFalse)) {
            // JLS §15.25.3 a reference conditional with a lambda or method reference operand
            branch.drop(constraint)
                    .add(conditionCheck)
                    .add(new JTypeConstraints.ExpressionCompatible(whenTrue, constraint.middle(), constraint.right()))
                    .add(new JTypeConstraints.ExpressionCompatible(whenFalse, constraint.middle(), constraint.right()));
            return;
        }

        List<JStandaloneType> trueCandidates = JStandaloneTyping.resolve(whenTrue, system);
        List<JStandaloneType> falseCandidates = trueCandidates.isEmpty() ? List.of() : JStandaloneTyping.resolve(whenFalse, system);

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        boolean addedPoly = false;
        for (JStandaloneType t : trueCandidates) {
            for (JStandaloneType f : falseCandidates) {
                boolean primitiveLike = isPrimitiveLike(whenTrue, t, system) && isPrimitiveLike(whenFalse, f, system)
                        && (bothBoolean(t.type(), f.type(), system) || bothNumeric(t.type(), f.type(), system));
                if (primitiveLike || !polyContext) {
                    // JLS §15.25.1 boolean, §15.25.2 numeric, standalone §15.25.3 reference conditional
                    JExpressionInformation.Conditional.resultType(whenTrue, t.type(), whenFalse, f.type(), system)
                            .map(r -> r instanceof JClassType ct && r.isProperType() ? JTypeRelations.capture(ct) : r)
                            .ifPresent(resultType -> newChildren.add(JReduceOperands.snapshot(List.of(t, f), List.of(conditionCheck), cond, resultType, constraint)));
                } else {
                    addedPoly = true;
                }
            }
        }

        if (addedPoly && newChildren.isEmpty()) {
            // JLS §15.25.3 a poly reference conditional pushes its target type to both operands
            branch.drop(constraint)
                    .add(conditionCheck)
                    .add(new JTypeConstraints.ExpressionCompatible(whenTrue, constraint.middle(), constraint.right()))
                    .add(new JTypeConstraints.ExpressionCompatible(whenFalse, constraint.middle(), constraint.right()));
        } else if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static boolean isInherentlyPoly(JExpressionInformation expr) {
        return expr instanceof JExpressionInformation.Lambda ||
                expr instanceof JExpressionInformation.InvocationReference<?> ||
                expr instanceof JExpressionInformation.InstantiationReference;
    }

    private static boolean isPrimitiveLike(JExpressionInformation expr, JStandaloneType resolved, JTypeSystem system) {
        if (expr instanceof JExpressionInformation.MethodInvocation<?> && resolved.branch() != null) {
            for (JTypeContext.ChosenMethod cm : resolved.branch().metadata().all(JTypeContext.ChosenMethod.class)) {
                if (cm.expression() == expr) {
                    // JLS §15.25 a method invocation is numeric or boolean by its declared return type
                    return JOperatorTypes.asPrimitive(cm.chosen().returnType(), system).isPresent();
                }
            }
        }
        return JOperatorTypes.asPrimitive(resolved.type(), system).isPresent();
    }

    private static boolean bothBoolean(JType a, JType b, JTypeSystem system) {
        return JOperatorTypes.asPrimitive(a, system).filter(p -> JOperatorTypes.isBoolean(p, system)).isPresent()
                && JOperatorTypes.asPrimitive(b, system).filter(p -> JOperatorTypes.isBoolean(p, system)).isPresent();
    }

    private static boolean bothNumeric(JType a, JType b, JTypeSystem system) {
        return JOperatorTypes.asPrimitive(a, system).filter(p -> !JOperatorTypes.isBoolean(p, system)).isPresent()
                && JOperatorTypes.asPrimitive(b, system).filter(p -> !JOperatorTypes.isBoolean(p, system)).isPresent();
    }
}
