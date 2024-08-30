package honeyroasted.jype.system.solver.constraints.compatibility;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class CompatibleLooseInvocation extends ConstraintMapper.Unary<TypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        return status.isUnknown() && (constraint.middle() == LOOSE_INVOCATION || constraint.middle() == ASSIGNMENT) &&
                left.isProperType() && right.isProperType();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        Set<Constraint> newChildren = new LinkedHashSet<>();

        newChildren.add(new TypeConstraints.Subtype(left, right));
        if (left instanceof PrimitiveType l && !(right instanceof PrimitiveType)) {
            newChildren.add(new TypeConstraints.Subtype(l.box(), right));
        } else if (right instanceof PrimitiveType r && !(left instanceof PrimitiveType)) {
            newChildren.add(new TypeConstraints.Subtype(left, r.box()));
        }

        branch.drop(constraint).diverge(newChildren);
    }
}
