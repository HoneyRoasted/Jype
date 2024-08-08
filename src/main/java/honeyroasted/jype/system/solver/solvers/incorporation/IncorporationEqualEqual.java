package honeyroasted.jype.system.solver.solvers.incorporation;

import honeyroasted.jype.system.solver._old.solvers.inference.MetaVarTypeResolver;
import honeyroasted.jype.system.solver.bounds.BinaryTypeBoundMapper;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;

public class IncorporationEqualEqual implements BinaryTypeBoundMapper<TypeBound.Equal, TypeBound.Equal> {

    @Override
    public boolean commutative() {
        return false;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder leftBuild, TypeBound.Equal leftBound, TypeBound.Result.Builder rightBuilder, TypeBound.Equal rightBound) {
        addAll(context.defaultConsumer(),
                leftBuild, rightBuilder);

        if (leftBound.hasMetaVar()) {
            MetaVarType mvt = leftBound.getMetaVar().orElse(null);
            Type otherType = leftBound.getOtherType().orElse(null);
            MetaVarTypeResolver subResolver = new MetaVarTypeResolver(Map.of(mvt, otherType));

            if (rightBound.left().typeEquals(mvt)) {
                //Case where alpha = S and alpha = T => S = T (18.3.1, Bullet #1)
                context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(otherType, rightBound.right()), TypeBound.Result.Propagation.AND,
                        leftBuild, rightBuilder));
            } else if (rightBound.right().typeEquals(mvt)) {
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
