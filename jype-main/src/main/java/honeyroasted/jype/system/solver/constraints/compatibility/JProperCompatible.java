package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

public class JProperCompatible extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isProperType() && constraint.right().isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        branch.set(constraint, Constraint.Status.known(JTypeRelations.isCompatible(constraint.left(), constraint.middle(), constraint.right())));
    }
}
