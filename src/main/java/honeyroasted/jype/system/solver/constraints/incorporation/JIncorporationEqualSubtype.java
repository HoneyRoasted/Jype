package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.Map;

public class JIncorporationEqualSubtype extends ConstraintMapper.Binary<JTypeConstraints.Equal, JTypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal leftConstraint, Constraint.Status leftStatus, JTypeConstraints.Subtype rightConstraint, Constraint.Status rightStatus) {
        JType ll = leftConstraint.left(), lr = leftConstraint.right(),
                rl = rightConstraint.left(), rr = rightConstraint.right();

        if (ll instanceof JMetaVarType || lr instanceof JMetaVarType) {
            JMetaVarType mvt = (JMetaVarType) (ll instanceof JMetaVarType ? ll : lr);
            JType otherType = ll instanceof JMetaVarType ? lr : ll;
            JMetaVarTypeResolveVisitor subResolver = new JMetaVarTypeResolveVisitor(Map.of(mvt, otherType));

            if (rl.typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                addSubtypeBound(branch, leftConstraint, otherType, rightConstraint, rr);
            } else if (rr.typeEquals(mvt)) {
                addSubtypeBound(branch, rightConstraint, rl, leftConstraint, otherType);
            } else {
                JType rlRes = subResolver.visit(rl);
                JType rrRes = subResolver.visit(rr);
                if (!rlRes.structuralEquals(rl) || !rrRes.structuralEquals(rr)) {
                    addSubtypeBound(branch, leftConstraint, rlRes, rightConstraint, rlRes);
                }
            }
        }
    }

    private static void addSubtypeBound(ConstraintBranch branch, Constraint leftCons, JType left, Constraint rightCons, JType right) {
        if (left.isProperType() && right.isProperType()) {
            boolean subtype = left.isAssignableTo(right);
            if (subtype) {
                branch.add(JTypeConstraints.Subtype.createBound(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Subtype(left, right), Constraint.Status.FALSE);
                branch.add(new JTypeConstraints.Contradiction(leftCons, rightCons), Constraint.Status.FALSE);
            }
        } else {
            branch.add(JTypeConstraints.Subtype.createBound(left, right), Constraint.Status.ASSUMED);
        }
    }
}
