package honeyroasted.jype.system.expression;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JInstantiableType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface JExpressionInformation {

    static SimplyTyped of(JType type) {
        return SimplyTyped.of(type);
    }

    String simpleName();

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
        return Optional.empty();
    }

    interface SimplyTyped extends JExpressionInformation {

        static SimplyTyped of(JType type) {
            return new Just(type);
        }

        record Just(JType type) implements SimplyTyped {
            @Override
            public String simpleName() {
                return "expr(" + this.type.simpleName() + ")";
            }

            @Override
            public JType type(JTypeSystem system) {
                return this.type;
            }

            @Override
            public String toString() {
                return "simply-typed(" + this.type + ")";
            }
        }

        JType type(JTypeSystem system);

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(this.type(system)));
        }
    }

    interface Constant extends SimplyTyped {

        Object value();

        @Override
        default JType type(JTypeSystem system) {
            if (value() == null) {
                return system.constants().nullType();
            } else {
                JType type = system.tryResolve(value().getClass());
                JType unboxed = system.constants().primitiveByBox().get(type);
                if (unboxed != null) {
                    return unboxed;
                }
                return type;
            }
        }
    }

    interface Multi extends JExpressionInformation {
        List<? extends JExpressionInformation> children();

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(JExpressionInformation::isSimplyTyped);
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().voidType());
            } else if (this.children().size() == 1) {
                return this.children().get(0).getSimpleType(system, Function.identity());
            } else {
                Set<JType> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    JType childType = c.getSimpleType(system, Function.identity()).get();

                    if (childType instanceof JIntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                return Optional.of(mapper.apply(JIntersectionType.of(childrenTypes, system)));
            }
        }
    }


    interface Invocation extends JExpressionInformation {
        JClassReference declaring();

        List<JExpressionInformation> parameters();

        List<JArgumentType> explicitTypeArguments();
    }

    interface Instantiation extends Invocation {
        JClassReference type();
    }

    interface MethodInvocation<T> extends Invocation {
        T source();

        String name();
    }

    interface Lambda extends JExpressionInformation {
        JExpressionInformation body();

        List<JType> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends JExpressionInformation {
        JInstantiableType type();

        List<JArgumentType> explicitTypeArguments();
    }

    interface InvocationReference extends JExpressionInformation {
        JExpressionInformation source();

        String methodName();

        List<JArgumentType> explicitTypeArguments();
    }

}
