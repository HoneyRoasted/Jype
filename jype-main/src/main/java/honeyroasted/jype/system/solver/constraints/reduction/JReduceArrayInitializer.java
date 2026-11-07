package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

public class JReduceArrayInitializer extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.ArrayInitializer;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.ArrayInitializer initializer = (JExpressionInformation.ArrayInitializer) constraint.left();
        JType target = constraint.right();

        if (!(target instanceof JArrayType arrayType)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JType component = arrayType.component();
        branch.drop(constraint);
        for (JExpressionInformation element : initializer.elements()) {
            branch.add(new JTypeConstraints.ExpressionCompatible(element, JTypeConstraints.Compatible.Context.ASSIGNMENT, component));
        }
    }
}
