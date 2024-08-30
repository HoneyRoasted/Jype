package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceSimplyTypedExpression extends ConstraintMapper.Unary<TypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left().isSimplyTyped();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        branch.drop(constraint).add(new TypeConstraints.Compatible(constraint.left().getSimpleType(constraint.right().typeSystem(), mapper).get(), constraint.middle(), constraint.right()));
    }
}
