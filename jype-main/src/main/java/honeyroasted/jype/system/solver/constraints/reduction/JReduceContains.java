package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

public class JReduceContains extends ConstraintMapper.Unary<JTypeConstraints.Contains> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        return status.isUnknown();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Contains constraint, Constraint.Status status) {
        JType s = constraint.left();
        JType t = constraint.right();

        if (t instanceof JWildType) {
            if (t instanceof JWildType.Upper wtu) {
                if (wtu.hasDefaultBounds()) {
                    branch.set(constraint, Constraint.Status.FALSE);
                } else {
                    if (s instanceof JWildType) {
                        if (s instanceof JWildType.Upper swtu) {
                            if (swtu.hasDefaultBounds()) {
                                branch.drop(constraint).add(new JTypeConstraints.Subtype(s.typeSystem().constants().object(), wtu.upperBound()));
                            } else {
                                branch.drop(constraint).add(new JTypeConstraints.Subtype(swtu.upperBound(), wtu.upperBound()));
                            }
                        } else if (s instanceof JWildType.Lower swtl) {
                            branch.drop(constraint).add(new JTypeConstraints.Equal(s.typeSystem().constants().object(), wtu.upperBound()));
                        }
                    } else {
                        branch.drop(constraint).add(new JTypeConstraints.Subtype(s, wtu.upperBound()));
                    }
                }
            } else if (t instanceof JWildType.Lower wtl) {
                if (s instanceof JWildType) {
                    if (s instanceof JWildType.Lower swtl) {
                        branch.drop(constraint).add(new JTypeConstraints.Subtype(wtl.lowerBound(), swtl.lowerBound()));
                    } else {
                        branch.set(constraint, Constraint.Status.FALSE);
                    }
                } else {
                    branch.drop(constraint).add(new JTypeConstraints.Subtype(wtl.lowerBound(), s));
                }
            }
        } else {
            if (s instanceof JWildType) {
                branch.set(constraint, Constraint.Status.FALSE);
            } else {
                branch.drop(constraint).add(new JTypeConstraints.Equal(s, t));
            }
        }
    }
}
