package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public class JSubtypeIntersection extends ConstraintMapper.Unary<JTypeConstraints.Subtype> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof JIntersectionType || right instanceof JIntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType left = mapper.apply(constraint.left());
        JType right = mapper.apply(constraint.right());

        if (left instanceof JIntersectionType l) {
            branch.drop(constraint)
                    .diverge(l.children().stream().map(t -> new JTypeConstraints.Subtype(t, right)).toList());
        } else if (right instanceof JIntersectionType r) {
            branch.drop(constraint);
            r.children().forEach(t -> branch.add(new JTypeConstraints.Subtype(left, t)));
        }
    }
}
