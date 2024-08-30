package honeyroasted.jype.system.expression;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface ExpressionInformation {

    static SimplyTyped of(Type type) {
        return SimplyTyped.of(type);
    }

    String simpleName();

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<Type> getSimpleType(TypeSystem system, Function<Type, Type> mapper) {
        return Optional.empty();
    }

    interface SimplyTyped extends ExpressionInformation {

        static SimplyTyped of(Type type) {
            return new Just(type);
        }

        record Just(Type type) implements SimplyTyped {
            @Override
            public String simpleName() {
                return "expr(" + this.type.simpleName() + ")";
            }

            @Override
            public Type type(TypeSystem system) {
                return this.type;
            }

            @Override
            public String toString() {
                return "simply-typed(" + this.type + ")";
            }
        }

        Type type(TypeSystem system);

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<Type> getSimpleType(TypeSystem system, Function<Type, Type> mapper) {
            return Optional.of(mapper.apply(this.type(system)));
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
        List<? extends ExpressionInformation> children();

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(ExpressionInformation::isSimplyTyped);
        }

        @Override
        default Optional<Type> getSimpleType(TypeSystem system, Function<Type, Type> mapper) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().voidType());
            } else if (this.children().size() == 1) {
                return this.children().get(0).getSimpleType(system, Function.identity());
            } else {
                Set<Type> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    Type childType = c.getSimpleType(system, Function.identity()).get();

                    if (childType instanceof IntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                return Optional.of(mapper.apply(IntersectionType.of(childrenTypes, system)));
            }
        }
    }


    interface Invocation extends ExpressionInformation {
        ClassReference declaring();

        List<ExpressionInformation> parameters();

        List<ArgumentType> explicitTypeArguments();
    }

    interface Instantiation extends Invocation {
        ClassReference type();
    }

    interface MethodInvocation<T> extends Invocation {
        T source();

        String name();
    }

    interface Lambda extends ExpressionInformation {
        ExpressionInformation body();

        List<Type> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends ExpressionInformation {
        InstantiableType type();

        List<ArgumentType> explicitTypeArguments();
    }

    interface InvocationReference extends ExpressionInformation {
        ExpressionInformation source();

        String methodName();

        List<ArgumentType> explicitTypeArguments();
    }

}
