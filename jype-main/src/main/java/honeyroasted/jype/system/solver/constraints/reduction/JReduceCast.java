package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.compatibility.JExpressionAssignmentConstant;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JReduceCast extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.Cast && !JExpressionAssignmentConstant.applies(constraint);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Cast cast = (JExpressionInformation.Cast) constraint.left();
        JType castType = cast.castType();
        JExpressionInformation operand = cast.operand();
        JTypeConstraints.Compatible result = new JTypeConstraints.Compatible(castType, constraint.middle(), constraint.right());

        if (operand instanceof JExpressionInformation.Lambda || operand instanceof JExpressionInformation.InvocationReference<?> || operand instanceof JExpressionInformation.InstantiationReference) {
            // JLS §15.16 a lambda or method reference operand targets the cast type
            branch.drop(constraint)
                    .add(new JTypeConstraints.ExpressionCompatible(operand, JTypeConstraints.Compatible.Context.ASSIGNMENT, castType))
                    .add(result);
            return;
        }

        // JLS §15.16 any other operand is a standalone expression cast by §5.5
        List<ConstraintBranch.Snapshot> children = new ArrayList<>();
        for (JStandaloneType resolved : JStandaloneTyping.resolve(operand, castType.typeSystem())) {
            Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
            constraints.put(new JTypeConstraints.Compatible(resolved.type(), JTypeConstraints.Compatible.Context.EXPLICIT_CAST, castType), Constraint.Status.UNKNOWN);
            constraints.put(result, Constraint.Status.UNKNOWN);
            if (resolved.extra() != null) {
                constraints.put(resolved.extra(), Constraint.Status.UNKNOWN);
            }
            ConstraintBranch.Snapshot snapshot = new ConstraintBranch.Snapshot(new PropertySet(), constraints);
            children.add(resolved.branch() == null ? snapshot : JStandaloneTyping.combine(resolved.branch(), snapshot));
        }
        if (children.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            branch.drop(constraint).divergeBranches(children);
        }
    }
}
