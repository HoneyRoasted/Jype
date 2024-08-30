package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class IncorporationSubtypeSubtype extends ConstraintMapper.Binary<TypeConstraints.Subtype, TypeConstraints.Subtype> {
    @Override
    protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype constraint, Constraint.Status status) {
        return status.isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, TypeConstraints.Subtype leftConstraint, Constraint.Status leftStatus, TypeConstraints.Subtype rightConstraint, Constraint.Status rightStatus) {
        Function<Type, Type> mapper = allContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);
        Type ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof MetaVarType mvt && mvt.typeEquals(rr)) {
            //Case where S <: alpha and alpha <: T => S <: T (18.3.1, Bullet #4)
            branch.add(new TypeConstraints.Subtype(lr, rl), Constraint.Status.ASSUMED);
        }

        if (ll instanceof MetaVarType mvt && mvt.typeEquals(rl)) {
            //Case where alpha <: T and alpha <: S and generic supertype G of T and S exists => generic parameters
            // that aren't wildcards are equal (18.3.1, Last Paragraph)
            commonSupertypes(lr, rr).forEach(pair -> {
                if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                    for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                        Type left = pair.left().typeArguments().get(i);
                        Type right = pair.right().typeArguments().get(i);

                        if (!(left instanceof WildType) && !(right instanceof WildType)) {
                            branch.add(new TypeConstraints.Equal(left, right), Constraint.Status.ASSUMED);
                        }
                    }
                }
            });
        }
    }

    private List<Pair<ParameterizedClassType, ParameterizedClassType>> commonSupertypes(Type left, Type right) {
        List<Pair<ParameterizedClassType, ParameterizedClassType>> result = new ArrayList<>();

        Set<Type> leftSupers = left.typeSystem().operations().findAllKnownSupertypes(left);
        Set<Type> rightSupers = left.typeSystem().operations().findAllKnownSupertypes(right);

        for (Type leftSuper : leftSupers) {
            if (leftSuper instanceof ParameterizedClassType lct) {
                for (Type rightSuper : rightSupers) {
                    if (rightSuper instanceof ParameterizedClassType rct && lct.classReference().typeEquals(rct.classReference())) {
                        result.add(Pair.of(lct, rct));
                    }
                }
            }
        }

        return result;
    }
}
