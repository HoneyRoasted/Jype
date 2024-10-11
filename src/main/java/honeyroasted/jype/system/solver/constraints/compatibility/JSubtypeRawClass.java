package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;

public class JSubtypeRawClass extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof JClassType l && !l.hasTypeArguments() &&
                right instanceof JClassType r && !r.hasTypeArguments();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        JClassType l = (JClassType) left;
        JClassType r = (JClassType) right;

        branch.set(constraint, Constraint.Status.known(l.classReference().hasSupertype(r.classReference())));
    }
}
