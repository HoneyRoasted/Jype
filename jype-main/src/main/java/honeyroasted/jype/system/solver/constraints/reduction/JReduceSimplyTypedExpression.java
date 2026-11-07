package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;

public class JReduceSimplyTypedExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isSimplyTyped() && !hasDedicatedReducer(constraint.left()) && !honeyroasted.jype.system.solver.constraints.compatibility.JExpressionAssignmentConstant.applies(constraint);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        branch.drop(constraint).add(new JTypeConstraints.Compatible(constraint.left().getSimpleType(constraint.right().typeSystem(), branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get(), constraint.middle(), constraint.right()));
    }

    public static boolean hasDedicatedReducer(JExpressionInformation expr) {
        return expr instanceof JExpressionInformation.Cast.Simple || expr instanceof JExpressionInformation.InstanceOf.Simple || expr instanceof JExpressionInformation.ArrayCreation.Simple;
    }
}
