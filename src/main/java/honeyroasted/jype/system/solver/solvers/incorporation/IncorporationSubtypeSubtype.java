package honeyroasted.jype.system.solver.solvers.incorporation;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.solver.bounds.BinaryTypeBoundMapper;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IncorporationSubtypeSubtype implements BinaryTypeBoundMapper<TypeBound.Subtype, TypeBound.Subtype> {
    @Override
    public void map(Context context, TypeBound.Result.Builder leftConstraint, TypeBound.Subtype leftBound, TypeBound.Result.Builder rightConstraint, TypeBound.Subtype rightBound) {
        addAll(context.defaultConsumer(),
                leftConstraint, rightConstraint);

        if (leftBound.left() instanceof MetaVarType mvt && mvt.typeEquals(rightBound.right())) {
            //Case where S <: alpha and alpha <: T => S <: T (18.3.1, Bullet #4)
            context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(leftBound.right(), rightBound.left()), leftConstraint, rightConstraint));
        }

        if (leftBound.left() instanceof MetaVarType mvt && mvt.typeEquals(rightBound.left())) {
            //Case where alpha <: T and alpha <: S and generic supertype G of T and S exists => generic parameters
            // that aren't wildcards are equal (18.3.1, Last Paragraph)
            commonSupertypes(leftBound.right(), rightBound.right()).forEach(pair -> {
                if (pair.left().typeArguments().size() == pair.right().typeArguments().size()) {
                    for (int i = 0; i < pair.left().typeArguments().size(); i++) {
                        Type left = pair.left().typeArguments().get(i);
                        Type right = pair.right().typeArguments().get(i);

                        if (!(left instanceof WildType) && !(right instanceof WildType)) {
                            context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(pair.left().typeArguments().get(i), pair.right().typeArguments().get(i)),
                                    TypeBound.Result.Propagation.AND, leftConstraint, rightConstraint));
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
