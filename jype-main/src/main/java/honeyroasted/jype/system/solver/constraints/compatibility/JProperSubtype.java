package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

public class JProperSubtype extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isProperType() && constraint.right().isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        branch.set(constraint, Constraint.Status.known(JTypeRelations.isSubtype(constraint.left(), constraint.right())));
    }
}
