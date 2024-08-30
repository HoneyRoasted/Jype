package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;
import java.util.function.Function;

public class IncorporationEqualSubtype extends ConstraintMapper.Binary<TypeConstraints.Equal, TypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Equal constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Equal leftConstraint, Constraint.Status leftStatus, TypeConstraints.Subtype rightConstraint, Constraint.Status rightStatus) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof MetaVarType || lr instanceof MetaVarType) {
            MetaVarType mvt = (MetaVarType) (ll instanceof MetaVarType ? ll : lr);
            Type otherType = ll instanceof MetaVarType ? lr : ll;
            MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));

            if (rl.typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                branch.add(new TypeConstraints.Subtype(otherType, rr), Constraint.Status.ASSUMED);
            } else if (rr.typeEquals(mvt)) {
                branch.add(new TypeConstraints.Subtype(rl, otherType), Constraint.Status.ASSUMED);
            } else {
                branch.add(new TypeConstraints.Subtype(subResolver.visit(rl), subResolver.visit(rr)), Constraint.Status.ASSUMED);
            }
        }
    }
}
