package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class SubtypeNone extends ConstraintMapper.Unary<TypeConstraints.Subtype> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && left.isProperType() && right.isProperType() &&
                (left instanceof NoneType || right instanceof NoneType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (right instanceof NoneType) {
            branch.setStatus(constraint, Constraint.Status.TRUE);
        } else if (left instanceof NoneType) {
            branch.setStatus(constraint, Constraint.Status.known(left.isNullType() && !(right instanceof PrimitiveType)));
        }
    }
}
