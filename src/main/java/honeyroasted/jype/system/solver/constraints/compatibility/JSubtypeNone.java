package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

public class JSubtypeNone extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof JNoneType || right instanceof JNoneType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        
        JType left = constraint.left();
        JType right = constraint.right();

        if (right instanceof JNoneType) {
            branch.set(constraint, Constraint.Status.TRUE);
        } else if (left instanceof JNoneType) {
            branch.set(constraint, Constraint.Status.known(left.isNullType() && !(right instanceof JPrimitiveType)));
        }
    }
}
