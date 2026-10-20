package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Reduces a conditional expression (JLS §15.25). If either branch is an inherently poly form
 * (a lambda or method/constructor reference - one that cannot be typed at all without a target
 * type), this is itself a poly conditional expression: the outer context is propagated straight
 * through to both branches, per JLS. Otherwise both branches are resolved to concrete types -
 * directly if simply typed, or via their own sub-solve otherwise, exactly like {@link
 * JReduceBinaryOperator}'s operands - and merged with {@link JExpressionInformation.Conditional#resultType}.
 * Either way, the condition itself must be boolean-compatible.
 */
public class JReduceConditionalExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Conditional;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Conditional cond = (JExpressionInformation.Conditional) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JExpressionInformation condition = cond.condition();
        JExpressionInformation whenTrue = cond.whenTrue();
        JExpressionInformation whenFalse = cond.whenFalse();

        Constraint conditionCheck = new JTypeConstraints.ExpressionCompatible(condition, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, system.constants().booleanType());

        if (isInherentlyPoly(whenTrue) || isInherentlyPoly(whenFalse)) {
            branch.drop(constraint)
                    .add(conditionCheck)
                    .add(new JTypeConstraints.ExpressionCompatible(whenTrue, constraint.middle(), constraint.right()))
                    .add(new JTypeConstraints.ExpressionCompatible(whenFalse, constraint.middle(), constraint.right()));
            return;
        }

        List<Resolved> trueCandidates = resolveOperand(whenTrue, system);
        List<Resolved> falseCandidates = resolveOperand(whenFalse, system);

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        for (Resolved t : trueCandidates) {
            for (Resolved f : falseCandidates) {
                JExpressionInformation.Conditional.resultType(whenTrue, t.type(), whenFalse, f.type(), system)
                        .ifPresent(resultType -> newChildren.add(combine(t, f, resultType, conditionCheck, constraint)));
            }
        }

        if (!newChildren.isEmpty()) {
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

    /**
     * Mirrors {@link JReduceBinaryOperator}'s operand resolution: a simply-typed branch resolves
     * directly (re-threading its own {@code ExpressionCompatible} constraint for legality), while
     * anything else is resolved via its own sub-solve against a fresh metavariable.
     */
    private record Resolved(ConstraintBranch branch, Constraint extra, JType type) {
    }

    private static List<Resolved> resolveOperand(JExpressionInformation operand, JTypeSystem system) {
        if (operand.isSimplyTyped()) {
            JType type = operand.getSimpleType(system, Function.identity()).get();
            Constraint extra = new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, type);
            return List.of(new Resolved(null, extra, type));
        }

        JMetaVarType target = system.typeFactory().newMetaVarType("BRANCH");
        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, target))
                .solve();

        List<Resolved> result = new ArrayList<>();
        solved.branches().forEach(childBranch -> {
            if (childBranch.status().isTrue()) {
                JType inst = JResolveBounds.findInstantiation(target, childBranch);
                inst = JMemberLookup.captureConvert(childBranch, inst); // JLS §5.1.10
                childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(target));
                result.add(new Resolved(childBranch, null, inst));
            }
        });
        return result;
    }

    private static ConstraintBranch.Snapshot combine(Resolved trueCandidate, Resolved falseCandidate, JType resultType, Constraint conditionCheck, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
        constraints.put(conditionCheck, Constraint.Status.UNKNOWN);
        constraints.put(new JTypeConstraints.Compatible(resultType, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        if (trueCandidate.extra() != null) constraints.putIfAbsent(trueCandidate.extra(), Constraint.Status.UNKNOWN);
        if (falseCandidate.extra() != null) constraints.putIfAbsent(falseCandidate.extra(), Constraint.Status.UNKNOWN);

        ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet(), constraints);
        if (trueCandidate.branch() != null) snapshot = combine(trueCandidate.branch(), snapshot);
        if (falseCandidate.branch() != null) snapshot = combine(falseCandidate.branch(), snapshot);
        return snapshot;
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
