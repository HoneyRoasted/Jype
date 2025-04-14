package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class JCompatibleStrictInvocation extends ConstraintMapper.Unary<JTypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && constraint.middle() == STRICT_INVOCATION && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Compatible constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        branch.drop(constraint)
                .add(new JTypeConstraints.Subtype(left, right));
    }
}
