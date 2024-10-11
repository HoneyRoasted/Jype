package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

public class JSubtypeIntersection extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        JType left = constraint.left();
        JType right = constraint.right();

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof JIntersectionType || right instanceof JIntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        
        JType left = constraint.left();
        JType right = constraint.right();

        if (left instanceof JIntersectionType l) {
            branch.drop(constraint)
                    .diverge(l.children().stream().map(t -> new JTypeConstraints.Subtype(t, right)).toList());
        } else if (right instanceof JIntersectionType r) {
            branch.drop(constraint);
            r.children().forEach(t -> branch.add(new JTypeConstraints.Subtype(left, t)));
        }
    }
}
