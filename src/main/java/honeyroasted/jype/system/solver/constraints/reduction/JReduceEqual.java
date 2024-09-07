package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public class JReduceEqual extends ConstraintMapper.Unary<JTypeConstraints.Equal> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType s = mapper.apply(constraint.left());
        JType t = mapper.apply(constraint.right());

        if (s.isProperType() && t.isProperType()) {
            branch.set(constraint, Constraint.Status.known(s.typeEquals(t)));
        } else if (s.isNullType() || t.isNullType()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else if ((t instanceof JMetaVarType && !(s instanceof JPrimitiveType)) ||
                (s instanceof JMetaVarType && !(t instanceof JPrimitiveType))) {
            branch.set(constraint, Constraint.Status.ASSUMED);
        }
    }
}
