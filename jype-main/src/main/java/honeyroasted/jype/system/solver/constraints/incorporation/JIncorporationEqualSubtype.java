package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

public class JIncorporationEqualSubtype extends ConstraintMapper.Binary<JTypeConstraints.Equal, JTypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isTrue() && JIncorporationSupport.isBound(constraint);
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue() && JIncorporationSupport.isBound(constraint);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal left, Constraint.Status leftStatus, JTypeConstraints.Subtype right, Constraint.Status rightStatus) {
        apply(branch, left, left.left(), left.right(), right);
        apply(branch, left, left.right(), left.left(), right);
    }

    private static void apply(ConstraintBranch branch, JTypeConstraints.Equal bound, JType alphaSide, JType s, JTypeConstraints.Subtype other) {
        if (!(alphaSide instanceof JMetaVarType alpha)) {
            return;
        }
        if (other.left().typeEquals(alpha)) {
            // JLS §18.3.1 α = S and α <: T imply ‹S <: T›
            JIncorporationSupport.deriveSubtype(branch, bound, other, s, other.right());
        } else if (other.right().typeEquals(alpha)) {
            // JLS §18.3.1 α = S and T <: α imply ‹T <: S›
            JIncorporationSupport.deriveSubtype(branch, bound, other, other.left(), s);
        } else if (!JIncorporationSupport.mentions(s, alpha) && (JIncorporationSupport.mentions(other.left(), alpha) || JIncorporationSupport.mentions(other.right(), alpha))) {
            // JLS §18.3.1 α = U and S <: T imply ‹S[α:=U] <: T[α:=U]›
            JIncorporationSupport.deriveSubtype(branch, bound, other, JIncorporationSupport.substitute(other.left(), alpha, s), JIncorporationSupport.substitute(other.right(), alpha, s));
        }
    }
}
