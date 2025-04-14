package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

public class JEqualType extends ConstraintMapper.Unary<JTypeConstraints.Equal> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        
        JType left = constraint.left();
        JType right = constraint.right();

        branch.set(constraint, Constraint.Status.known(left.typeEquals(right)));
    }
}
