package honeyroasted.jype.system.solver._old.solvers.inference.expression;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExpressionInformation {

    String simpleName();

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<Type> getSimpleType(TypeSystem system) {
        return Optional.empty();
    }

    interface SimplyTyped extends ExpressionInformation {
        Type type(TypeSystem system);

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<Type> getSimpleType(TypeSystem system) {
            return Optional.of(this.type(system));
        }
    }

    interface Constant extends SimplyTyped {

        Object value();

        @Override
        default Type type(TypeSystem system) {
            if (value() == null) {
                return system.constants().nullType();
            } else {
                Type type = system.tryResolve(value().getClass());
                Type unboxed = system.constants().primitiveByBox().get(type);
                if (unboxed != null) {
                    return unboxed;
                }
                return type;
            }
        }
    }

    interface Multi extends ExpressionInformation {
        List<ExpressionInformation> children();

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(e -> e instanceof SimplyTyped || (e instanceof Multi m && m.isSimplyTyped()));
        }

        @Override
        default Optional<Type> getSimpleType(TypeSystem system) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().nullType());
            } else if (this.children().size() == 1) {
                return this.children().get(0) instanceof SimplyTyped st ? Optional.of(st.type(system)) : this.children().get(0).getSimpleType(system);
            } else {
                IntersectionType type = new IntersectionTypeImpl(system);
                Set<Type> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    Type childType = null;
                    if (c instanceof SimplyTyped st) {
                        childType = st.type(system);
                    } else if (c instanceof Multi p) {
                        childType = p.getSimpleType(system).get();
                    }

                    if (childType instanceof IntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                type.setChildren(childrenTypes);
                type.setUnmodifiable(true);
                return Optional.of(type);
            }
        }
    }

    interface Instantiation extends ExpressionInformation {
        InstantiableType type();

        List<ExpressionInformation> parameters();

        List<Type> explicitTypeArguments();
    }

    interface Invocation extends ExpressionInformation {
        ExpressionInformation source();

        List<ExpressionInformation> parameters();

        List<Type> explicitTypeArguments();
    }

    interface Lambda extends ExpressionInformation {
        ExpressionInformation body();

        List<Type> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends ExpressionInformation {
        InstantiableType type();
    }

    interface InvocationReference extends ExpressionInformation {
        ExpressionInformation source();
        String methodName();
        List<Type> explicitTypeArguments();
    }

}
