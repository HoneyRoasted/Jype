package honeyroasted.jype.system.solver.bounds;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface UnaryTypeBoundMapper<T extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(Context context, TypeBound.Result.Builder... input) {
        map(context, input[0], (T) input[0].bound());
    }

    @Override
    default boolean accepts(TypeBound.Result.Builder constraint) {
        return type().isAssignableFrom(constraint.bound().getClass());
    }

    void map(Context context, TypeBound.Result.Builder constraint, T bound);

    private Type[] typeArgs() {
        for (Type inter : getClass().getGenericInterfaces()) {
            if (inter instanceof ParameterizedType pt) {
                if (pt.getRawType() instanceof Class<?> cls && cls == UnaryTypeBoundMapper.class) {
                    return pt.getActualTypeArguments();
                }
            }
        }

        return new Type[]{null, null};
    }

    default Class<T> type() {
        return (Class<T>) this.typeArgs()[0];
    }

}
