package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeIntersection extends ConstraintMapper.Unary<TypeConstraints.Subtype> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof IntersectionType || right instanceof IntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left instanceof IntersectionType l) {
            branch.drop(constraint)
                    .diverge(l.children().stream().map(t -> new TypeConstraints.Subtype(t, right)).toList());
        } else if (right instanceof IntersectionType r) {
            branch.drop(constraint);
            r.children().forEach(t -> branch.add(new TypeConstraints.Subtype(left, t)));
        }
    }
}
