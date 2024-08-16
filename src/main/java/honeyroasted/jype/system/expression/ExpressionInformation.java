package honeyroasted.jype.system.expression;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.visitors.TypeMappingVisitor;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface ExpressionInformation {

    static SimplyTyped of(Type type) {
        return SimplyTyped.of(type);
    }

    String simpleName();

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<Type> getSimpleType(TypeSystem system) {
        return Optional.empty();
    }

    default <T> ExpressionInformation.Mapped<T> mapped(TypeMappingVisitor<T> mapper, Supplier<T> contextFactory) {
        return new Mapped<>(this, mapper, contextFactory);
    }

    class Mapped<T> implements ExpressionInformation {
        private ExpressionInformation delegate;
        private TypeMappingVisitor<T> mapper;
        private Supplier<T> contextFactory;

        public Mapped(ExpressionInformation delegate, TypeMappingVisitor<T> mapper, Supplier<T> contextFactory) {
            this.delegate = delegate;
            this.mapper = mapper;
            this.contextFactory = contextFactory;
        }

        public ExpressionInformation delegate() {
            return this.delegate;
        }

        @Override
        public String simpleName() {
            return this.delegate.simpleName();
        }

        @Override
        public boolean isSimplyTyped() {
            return this.delegate.isSimplyTyped();
        }

        @Override
        public Optional<Type> getSimpleType(TypeSystem system) {
            return this.delegate.getSimpleType(system).map(t -> this.mapper.visit(t, this.contextFactory.get()));
        }
    }

    interface SimplyTyped extends ExpressionInformation {

        static SimplyTyped of(Type type) {
            return new Just(type);
        }

        record Just(Type type) implements SimplyTyped {
            @Override
            public String simpleName() {
                return this.type.simpleName();
            }

            @Override
            public Type type(TypeSystem system) {
                return this.type;
            }

            @Override
            public String toString() {
                return "just(" + this.type + ")";
            }
        }

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
        List<? extends ExpressionInformation> children();

        Multi createNew(List<ExpressionInformation> children);

        @Override
        default <T> Mapped<T> mapped(TypeMappingVisitor<T> mapper, Supplier<T> contextFactory) {
            return new Mapped<>(this.createNew(this.children().stream().map(expr -> (ExpressionInformation) expr.mapped(mapper, contextFactory)).toList()),
                    mapper, contextFactory);
        }

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(ExpressionInformation::isSimplyTyped);
        }

        @Override
        default Optional<Type> getSimpleType(TypeSystem system) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().nullType());
            } else if (this.children().size() == 1) {
                return this.children().get(0).getSimpleType(system);
            } else {
                Set<Type> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    Type childType = c.getSimpleType(system).get();

                    if (childType instanceof IntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                return Optional.of(IntersectionType.of(childrenTypes, system));
            }
        }
    }

    interface Instantiation extends ExpressionInformation {
        ClassReference declaring();

        ClassReference type();

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
