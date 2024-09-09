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
        Function<JType, JType> mapper = allContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);
        JType ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof JMetaVarType || lr instanceof JMetaVarType) {
            JMetaVarType mvt = (JMetaVarType) (ll instanceof JMetaVarType ? ll : lr);
            JType otherType = ll instanceof JMetaVarType ? lr : ll;
            JMetaVarTypeResolver subResolver = new JMetaVarTypeResolver(Map.of(mvt, otherType));

            if (rl.typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                branch.add(JTypeConstraints.Subtype.createBound(otherType, rr), Constraint.Status.ASSUMED);
            } else if (rr.typeEquals(mvt)) {
                branch.add(JTypeConstraints.Subtype.createBound(rl, otherType), Constraint.Status.ASSUMED);
            } else {
                branch.add(JTypeConstraints.Subtype.createBound(subResolver.visit(rl), subResolver.visit(rr)), Constraint.Status.ASSUMED);
            }
        }
    }
}
