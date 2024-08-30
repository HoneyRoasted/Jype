package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class CompatibleExplicitCast extends ConstraintMapper.Unary<TypeConstraints.Compatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && constraint.middle() == EXPLICIT_CAST && left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        branch.drop(constraint)
                .diverge(new TypeConstraints.Compatible(constraint.left(), LOOSE_INVOCATION, constraint.right()),
                        new TypeConstraints.Compatible(constraint.right(), LOOSE_INVOCATION, constraint.left()));
    }
}
