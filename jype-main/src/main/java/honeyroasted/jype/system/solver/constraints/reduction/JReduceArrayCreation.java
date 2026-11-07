package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

public class JReduceArrayCreation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.ArrayCreation;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.ArrayCreation arrayCreation = (JExpressionInformation.ArrayCreation) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JType arrayType = arrayCreation.arrayType(system);

        branch.drop(constraint);
        for (JExpressionInformation dimension : arrayCreation.dimensions()) {
            branch.add(new JTypeConstraints.ExpressionCompatible(dimension, JTypeConstraints.Compatible.Context.ASSIGNMENT, system.constants().intType()));
        }
        if (arrayCreation.initializer() != null) {
            branch.add(new JTypeConstraints.ExpressionCompatible(arrayCreation.initializer(), JTypeConstraints.Compatible.Context.ASSIGNMENT, arrayType));
        }
        branch.add(new JTypeConstraints.Compatible(arrayType, constraint.middle(), constraint.right()));
    }
}
