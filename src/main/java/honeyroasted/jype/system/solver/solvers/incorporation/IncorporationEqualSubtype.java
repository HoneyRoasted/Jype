package honeyroasted.jype.system.solver.solvers.incorporation;

import honeyroasted.jype.system.solver.bounds.BinaryTypeBoundMapper;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;

public class IncorporationEqualSubtype implements BinaryTypeBoundMapper<TypeBound.Equal, TypeBound.Subtype>  {

    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.BOUND;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder leftBuild, TypeBound.Equal leftBound, TypeBound.Result.Builder rightBuilder, TypeBound.Subtype rightBound) {
        addAll(context.defaultConsumer(),
                leftBuild, rightBuilder);

        if (leftBound.hasMetaVar()) {
            MetaVarType mvt = context.view(leftBound.getMetaVar().orElse(null));
            Type otherType = context.view(leftBound.getOtherType().orElse(null));
            MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));

            if (context.view(rightBound.left()).typeEquals(mvt)) {
                //Case where alpha = S and alpha <: T => S <: T (18.3.1, Bullet #2)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(otherType, rightBound.right()), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            } else if (context.view(rightBound.right()).typeEquals(mvt)) {
                //Case where alpha = S and T <: alpha => T <: S (18.3.1, Bullet #3)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(rightBound.left(), otherType), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            } else {
                //Case where alpha = U and S <: T => S[alpha = U] <: T[alpha = U] (18.3.1, Bullet #6)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(subResolver.visit(rightBound.left()), subResolver.visit(rightBound.right())), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            }
        }
    }

}
