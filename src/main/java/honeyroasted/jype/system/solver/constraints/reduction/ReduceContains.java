package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.function.Function;

public class ReduceContains extends ConstraintMapper.Unary<TypeConstraints.Contains> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Contains constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Contains constraint, Constraint.Status status) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type s = mapper.apply(constraint.left());
        Type t = mapper.apply(constraint.right());

        if (t instanceof WildType) {
            if (t instanceof WildType.Upper wtu) {
                if (wtu.hasDefaultBounds()) {
                    branch.setStatus(constraint, Constraint.Status.FALSE);
                } else {
                    if (s instanceof WildType) {
                        if (s instanceof WildType.Upper swtu) {
                            if (swtu.hasDefaultBounds()) {
                                branch.drop(constraint).add(new TypeConstraints.Subtype(s.typeSystem().constants().object(), wtu.upperBound()));
                            } else {
                                branch.drop(constraint).add(new TypeConstraints.Subtype(swtu.upperBound(), wtu.upperBound()));
                            }
                        } else if (s instanceof WildType.Lower swtl) {
                            branch.drop(constraint).add(new TypeConstraints.Equal(s.typeSystem().constants().object(), wtu.upperBound()));
                        }
                    } else {
                        branch.drop(constraint).add(new TypeConstraints.Subtype(s, wtu.upperBound()));
                    }
                }
            } else if (t instanceof WildType.Lower wtl) {
                if (s instanceof WildType) {
                    if (s instanceof WildType.Lower swtl) {
                        branch.drop(constraint).add(new TypeConstraints.Subtype(wtl.lowerBound(), swtl.lowerBound()));
                    } else {
                        branch.setStatus(constraint, Constraint.Status.FALSE);
                    }
                } else {
                    branch.drop(constraint).add(new TypeConstraints.Subtype(wtl.lowerBound(), s));
                }
            }
        } else {
            if (s instanceof WildType) {
                branch.setStatus(constraint, Constraint.Status.FALSE);
            } else {
                branch.drop(constraint).add(new TypeConstraints.Equal(s, t));
            }
        }
    }
}
