package honeyroasted.jype.system.solver.constraints.incorporation;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class IncorporationSubtypeSubtype implements ConstraintMapper.Binary<TypeConstraints.Subtype, TypeConstraints.Subtype> {

    @Override
    public boolean filterLeft(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        return node.status() == ConstraintNode.Status.TRUE;
    }

    @Override
    public boolean filterRight(PropertySet context, ConstraintNode node, TypeConstraints.Subtype constraint) {
        return node.status() == ConstraintNode.Status.TRUE;
    }

    @Override
    public void process(PropertySet context, ConstraintNode leftNode, TypeConstraints.Subtype leftConstraint, ConstraintNode rightNode, TypeConstraints.Subtype rightConstraint) {
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(leftNode);
        Type ll = mapper.apply(leftConstraint.left()), lr = mapper.apply(leftConstraint.right()),
                rl = mapper.apply(rightConstraint.left()), rr = mapper.apply(rightConstraint.right());

        if (ll instanceof MetaVarType mvt && mvt.typeEquals(rr)) {
            //Case where S <: alpha and alpha <: T => S <: T (18.3.1, Bullet #4)
            leftNode.expandRoot(ConstraintNode.Operation.AND, false)
                    .attach(new TypeConstraints.Subtype(lr, rl).createLeaf().overrideStatus(true));
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
                            leftNode.expandRoot(ConstraintNode.Operation.AND, false)
                                    .attach(new TypeConstraints.Equal(left, right).createLeaf().overrideStatus(true));
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
