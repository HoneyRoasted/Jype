package honeyroasted.jype.system.solver.bounds;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface BinaryTypeBoundMapper<T extends TypeBound, K extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(Context context, TypeBound.Result.Builder... input) {
        map(context, input[0], (T) input[0].bound(), input[1], (K) input[1].bound());
    }

    @Override
    default boolean commutative() {
        return this.leftType() == this.rightType();
    }

    @Override
    default boolean accepts(TypeBound.Result.Builder builder) {
        return builder.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN &&
                ((leftType().isAssignableFrom(builder.bound().getClass()) && acceptsLeft(builder, (T) builder.bound())) ||
                        (rightType().isAssignableFrom(builder.bound().getClass())) && acceptsRight(builder, (K) builder.bound()));
    }

    default boolean acceptsLeft(TypeBound.Result.Builder builder, T bound) {
        return true;
    }

    default boolean acceptsRight(TypeBound.Result.Builder builder, K bound) {
        return true;
    }


    @Override
    default boolean accepts(TypeBound.Result.Builder... input) {
        return leftType().isAssignableFrom(input[0].bound().getClass()) &&
                rightType().isAssignableFrom(input[1].bound().getClass());
    }

    void map(Context context, TypeBound.Result.Builder leftBuild, T leftBound,
             TypeBound.Result.Builder rightBuilder, K rightBound);

    private Type[] typeArgs() {
        for (Type inter : getClass().getGenericInterfaces()) {
            if (inter instanceof ParameterizedType pt) {
                if (pt.getRawType() instanceof Class<?> cls && cls == BinaryTypeBoundMapper.class) {
                    return pt.getActualTypeArguments();
                }
            }
        }

        return new Type[]{null, null};
    }

    default Class<T> leftType() {
        return (Class<T>) typeArgs()[0];
    }

    default Class<T> rightType() {
        return (Class<T>) typeArgs()[1];
    }


}
