package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

public class JSubtypeWild extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof JWildType.Upper || right instanceof JWildType.Lower);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        if (left instanceof JWildType.Upper l) {
            branch.drop(constraint)
                    .diverge(l.upperBounds().stream().map(b -> new JTypeConstraints.Subtype(b, right)).toList());
        } else if (right instanceof JWildType.Lower r) {
            branch.drop(constraint);
            r.upperBounds().forEach(b -> branch.add(new JTypeConstraints.Subtype(left, b)));
        }
    }
}
