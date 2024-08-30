package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceCompatible extends ConstraintMapper.Unary<TypeConstraints.Compatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Compatible constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type left = mapper.apply(constraint.left());
        Type right = mapper.apply(constraint.right());

        if (left.isProperType() && right.isProperType()) {
            branch.setStatus(constraint, Constraint.Status.known(left.typeSystem().operations().isCompatible(left, right, constraint.middle())));
        } else if (left instanceof PrimitiveType pt) {
            branch.drop(constraint).add(new TypeConstraints.Compatible(pt.box(), constraint.middle(), right));
        } else if (right instanceof PrimitiveType pt) {
            branch.drop(constraint).add(new TypeConstraints.Compatible(left, constraint.middle(), pt.box()));
        } else if (left instanceof ClassType pct && pct.hasAnyTypeArguments() && right instanceof ClassType ct && !ct.hasTypeArguments() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), ct.classReference())) {
            branch.setStatus(constraint, Constraint.Status.TRUE);
        } else if (left instanceof ArrayType at && at.deepComponent() instanceof ClassType pct && pct.hasAnyTypeArguments() &&
                right instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() &&
                left.typeSystem().operations().isSubtype(pct.classReference(), rpct.classReference())) {
            branch.setStatus(constraint, Constraint.Status.TRUE);
        } else {
            branch.drop(constraint).add(new TypeConstraints.Subtype(left, right));
        }
    }
}
