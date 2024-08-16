package honeyroasted.jype.system.solver.solvers.incorporation;

import honeyroasted.jype.system.solver.bounds.BinaryTypeBoundMapper;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;

public class IncorporationEqualEqual implements BinaryTypeBoundMapper<TypeBound.Equal, TypeBound.Equal> {

    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.BOUND;
    }

    @Override
    public boolean commutative() {
        return false;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder leftBuild, TypeBound.Equal leftBound, TypeBound.Result.Builder rightBuilder, TypeBound.Equal rightBound) {
        addAll(context.defaultConsumer(),
                leftBuild, rightBuilder);

        if (leftBound.hasMetaVar()) {
            MetaVarType mvt = context.view(leftBound.getMetaVar().orElse(null));
            Type otherType = context.view(leftBound.getOtherType().orElse(null));
            MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));

            if (context.view(rightBound.left()).typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(otherType, rightBound.right()), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            } else if (context.view(rightBound.right()).typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(otherType, rightBound.left()), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            } else {
                //Case where alpha = U and S = T => S[alpha=U] = T[alpha=U] (18.3.1, Bullet #5)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(subResolver.visit(rightBound.left()), subResolver.visit(rightBound.right())), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            }
        }
    }
}
