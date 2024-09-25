package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolver;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.Map;
import java.util.function.Function;

public class JIncorporationEqualEqual extends ConstraintMapper.Binary<JTypeConstraints.Equal, JTypeConstraints.Equal> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal constraint, Constraint.Status status) {
        return super.filterRight(allContext, branchContext, branch, constraint, status);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.Equal leftConstraint, Constraint.Status leftStatus, JTypeConstraints.Equal rightConstraint, Constraint.Status rightStatus) {
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof JMetaVarType || lr instanceof JMetaVarType) {
            JMetaVarType mvt = (JMetaVarType) (ll instanceof JMetaVarType ? ll : lr);
            JType otherType = ll instanceof JMetaVarType ? lr : ll;
            JMetaVarTypeResolver subResolver = new JMetaVarTypeResolver(Map.of(mvt, otherType));

            if (rl.typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                addEqualBound(branch, leftConstraint, otherType, rightConstraint, rr);

                branch.add(JTypeConstraints.Equal.createBound(otherType, rr), Constraint.Status.ASSUMED);
            } else if (rr.typeEquals(mvt)) {
                addEqualBound(branch, leftConstraint, otherType, rightConstraint, rl);
            } else {
                JType rlRes = subResolver.visit(rl);
                JType rrRes = subResolver.visit(rr);
                if (!rlRes.structuralEquals(rl) || !rrRes.structuralEquals(rr)) {
                    addEqualBound(branch, leftConstraint, rlRes, rightConstraint, rlRes);
                }
            }
        }
    }

    private static void addEqualBound(ConstraintBranch branch, Constraint leftCons, JType left, Constraint rightCons, JType right) {
        if (left.isProperType() && right.isProperType()) {
            boolean equal = left.typeEquals(right);
            if (equal) {
                branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.TRUE);
            } else {
                branch.add(new JTypeConstraints.Equal(left, right), Constraint.Status.FALSE);
                branch.add(new JTypeConstraints.Contradiction(leftCons, rightCons), Constraint.Status.FALSE);
            }
        } else {
            branch.add(JTypeConstraints.Equal.createBound(left, right), Constraint.Status.ASSUMED);
        }
    }
}
