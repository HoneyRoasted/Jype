package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

public class JReduceCompatible extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        if (left.isProperType() && right.isProperType()) {
            branch.set(constraint, Constraint.Status.known(left.typeSystem().operations().isCompatible(left, right, constraint.middle())));
        } else if (left instanceof JPrimitiveType pt) {
            branch.drop(constraint).add(new JTypeConstraints.Compatible(pt.box(), constraint.middle(), right));
        } else if (right instanceof JPrimitiveType pt) {
            branch.drop(constraint).add(new JTypeConstraints.Compatible(left, constraint.middle(), pt.box()));
        } else if (left instanceof JClassType pct && pct.hasAnyTypeArguments() && right instanceof JClassType ct && !ct.hasTypeArguments() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), ct.classReference())) {
            branch.set(constraint, Constraint.Status.TRUE);
        } else if (left instanceof JArrayType at && at.deepComponent() instanceof JClassType pct && pct.hasAnyTypeArguments() &&
                right instanceof JArrayType rat && rat.deepComponent() instanceof JClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), rpct.classReference())) {
            branch.set(constraint, Constraint.Status.TRUE);
        } else {
            branch.drop(constraint).add(new JTypeConstraints.Subtype(left, right));
        }
    }
}
