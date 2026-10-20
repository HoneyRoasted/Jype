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
 * Reduces a binary operator expression (JLS §15.17-§15.24), mirroring {@link JReduceUnaryOperator}:
 * each operand's type is resolved directly when simply typed (re-threading its own
 * {@code ExpressionCompatible} constraint), or via its own sub-solve against a fresh metavariable
 * otherwise. {@link JExpressionInformation.BinaryOperator#resultType} is applied to every
 * combination of the two operands' candidate types (ordinarily just one each); a combination the
 * operator doesn't accept - or an operand sub-solve with no true branch - contributes nothing, and
 * the whole expression is false if no combination produces a result.
 */
public class JReduceBinaryOperator extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.BinaryOperator;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.BinaryOperator binary = (JExpressionInformation.BinaryOperator) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();

        List<Resolved> leftCandidates = resolveOperand(binary.left(), system);
        List<Resolved> rightCandidates = resolveOperand(binary.right(), system);

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        for (Resolved left : leftCandidates) {
            for (Resolved right : rightCandidates) {
                JExpressionInformation.BinaryOperator.resultType(binary.kind(), left.type(), right.type(), system)
                        .ifPresent(resultType -> newChildren.add(combine(left, right, resultType, constraint)));
            }
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    /**
     * An operand's resolved candidate type, plus however its own legality needs to be threaded
     * back into whatever snapshot consumes it: {@code extra} for an already-simply-typed operand
     * (re-asserted as its own {@code ExpressionCompatible}), or {@code branch} for one resolved via
     * its own sub-solve (whose already-verified constraints just get merged in).
     */
    private record Resolved(ConstraintBranch branch, Constraint extra, JType type) {
    }

    private static List<Resolved> resolveOperand(JExpressionInformation operand, JTypeSystem system) {
        if (operand.isSimplyTyped()) {
            JType type = operand.getSimpleType(system, Function.identity()).get();
            Constraint extra = new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, type);
            return List.of(new Resolved(null, extra, type));
        }

        JMetaVarType target = system.typeFactory().newMetaVarType("OPERAND");
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

    private static ConstraintBranch.Snapshot combine(Resolved left, Resolved right, JType resultType, JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
        constraints.put(new JTypeConstraints.Compatible(resultType, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        if (left.extra() != null) constraints.putIfAbsent(left.extra(), Constraint.Status.UNKNOWN);
        if (right.extra() != null) constraints.putIfAbsent(right.extra(), Constraint.Status.UNKNOWN);

        ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet(), constraints);
        if (left.branch() != null) snapshot = combine(left.branch(), snapshot);
        if (right.branch() != null) snapshot = combine(right.branch(), snapshot);
        return snapshot;
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }
}
