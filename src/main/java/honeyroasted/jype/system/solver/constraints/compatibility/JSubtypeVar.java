package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

public class JSubtypeVar extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof JVarType;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        JVarType l = (JVarType) left;
        branch.drop(constraint);
        l.upperBounds().stream().forEach(b -> branch.add(new JTypeConstraints.Subtype(b, right)));
    }
}
