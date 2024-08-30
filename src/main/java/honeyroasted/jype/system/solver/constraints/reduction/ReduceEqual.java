package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ReduceEqual extends ConstraintMapper.Unary<TypeConstraints.Equal> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Equal constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type s = mapper.apply(constraint.left());
        Type t = mapper.apply(constraint.right());

        if (s.isProperType() && t.isProperType()) {
            branch.setStatus(constraint, Constraint.Status.known(s.typeEquals(t)));
        } else if (s.isNullType() || t.isNullType()) {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        } else if ((t instanceof MetaVarType && !(s instanceof PrimitiveType)) ||
                (s instanceof MetaVarType && !(t instanceof PrimitiveType))) {
            branch.setStatus(constraint, Constraint.Status.ASSUMED);
        }
    }
}
