package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JReduceBinaryOperator extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.BinaryOperator;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.BinaryOperator binary = (JExpressionInformation.BinaryOperator) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();

        List<JStandaloneType> leftCandidates = JStandaloneTyping.resolve(binary.left(), system);
        List<JStandaloneType> rightCandidates = leftCandidates.isEmpty() ? List.of() : JStandaloneTyping.resolve(binary.right(), system);

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        for (JStandaloneType left : leftCandidates) {
            for (JStandaloneType right : rightCandidates) {
                // JLS §15.17-§15.24 binary operator result type
                JExpressionInformation.BinaryOperator.resultType(binary.kind(), left.type(), right.type(), system)
                        .ifPresent(resultType -> newChildren.add(JReduceOperands.snapshot(List.of(left, right), binary, resultType, constraint)));
            }
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }
}
