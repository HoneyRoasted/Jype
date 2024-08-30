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

public class SubtypeRawClass extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                left instanceof ClassType l && !l.hasTypeArguments() &&
                right instanceof ClassType r && !r.hasTypeArguments();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        ClassType l = (ClassType) left;
        ClassType r = (ClassType) right;

        branch.setStatus(constraint, Constraint.Status.known(l.classReference().hasSupertype(r.classReference())));
    }
}
