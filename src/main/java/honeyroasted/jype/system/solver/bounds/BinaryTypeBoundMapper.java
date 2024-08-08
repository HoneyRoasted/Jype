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
    default boolean accepts(TypeBound.Result.Builder constraint) {
        return leftType().isAssignableFrom(constraint.bound().getClass()) ||
                rightType().isAssignableFrom(constraint.bound().getClass());
    }

    @Override
    default boolean accepts(TypeBound.Result.Builder... input) {
        return leftType().isAssignableFrom(input[0].bound().getClass()) &&
                rightType().isAssignableFrom(input[1].bound().getClass());
    }

    void map(Context context, TypeBound.Result.Builder leftConstraint, T leftBound,
             TypeBound.Result.Builder rightConstraint, K rightBound);

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
