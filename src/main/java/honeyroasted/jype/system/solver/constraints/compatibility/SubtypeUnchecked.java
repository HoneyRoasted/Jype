package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeUnchecked extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof ClassType l && right instanceof ClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) || (!l.hasAnyTypeArguments() && r.hasAnyTypeArguments()));
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ClassType l = (ClassType) left;
        ClassType r = (ClassType) right;

        branch.drop(constraint)
                .add(new TypeConstraints.Subtype(l.classReference(), r.classReference()));
    }
}
