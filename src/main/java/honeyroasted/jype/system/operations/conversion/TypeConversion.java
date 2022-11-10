package honeyroasted.jype.system.operations.conversion;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.operations.AbstractTypeOperation;
import honeyroasted.jype.system.operations.BooleanTypeOperation;
import honeyroasted.jype.system.operations.TypeOperation;
import honeyroasted.jype.system.operations.result.TypeResult;
import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;
import honeyroasted.jype.type.TypePrimitive;
import honeyroasted.jype.type.TypeVariable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface TypeConversion extends TypeOperation<TypeConcrete> {

    class Identity extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Identity(TypeConcrete from, TypeConcrete to) {
            super(String.format("Identity conversion from {%s} to {%s}.", from, to),
                    from, to);
        }

        @Override
        public TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Equal(this.type(0), this.type(1)))
                    .build();
        }
    }

    class WideningPrimitive extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public static final Map<String, Set<String>> WIDENING_CONVERSIONS = Map.of(
                "Z", Set.of(),
                "B", Set.of("S", "I", "J", "F", "D"),
                "S", Set.of("I", "J", "F", "D"),
                "C", Set.of("I", "J", "F", "D"),
                "I", Set.of("J", "F", "D"),
                "J", Set.of("F", "D"),
                "F", Set.of("D"),
                "D", Set.of()
        );

        public WideningPrimitive(TypeConcrete from, TypeConcrete to) {
            super(String.format("Widening primitive conversion from {%s} to {%s}.", from, to),
                    from, to);
        }

        @Override
        public TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(0), TypePrimitive.class))
                    .and(new BooleanTypeOperation.Kind(this.type(1), TypePrimitive.class))
                    .and(() -> {
                        TypePrimitive from = this.type(0);
                        TypePrimitive to = this.type(1);
                        return WIDENING_CONVERSIONS.get(from.descriptor()).contains(to.descriptor());
                    })
                    .value(() -> this.type(1))
                    .build();
        }
    }

    class NarrowingPrimitive extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public static final Map<String, Set<String>> NARROWING_CONVERSIONS = Map.of(
                "Z", Set.of(),
                "B", Set.of(),
                "S", Set.of("B", "C"),
                "C", Set.of("B", "S"),
                "I", Set.of("B", "S", "C"),
                "J", Set.of("B", "S", "C", "I"),
                "F", Set.of("B", "S", "C", "I", "J"),
                "D", Set.of("B", "S", "C", "I", "J", "F")
        );
        public NarrowingPrimitive(TypeConcrete from, TypeConcrete to) {
            super(String.format("Narrowing primitive conversion from {%s} to {%s}.", from, to),
                    from, to);
        }

        @Override
        protected TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(0), TypePrimitive.class))
                    .and(new BooleanTypeOperation.Kind(this.type(1), TypePrimitive.class))
                    .and(() -> {
                        TypePrimitive from = this.type(0);
                        TypePrimitive to = this.type(1);
                        return NARROWING_CONVERSIONS.get(from.descriptor()).contains(to.descriptor());
                    })
                    .value(() -> this.type(1))
                    .build();
        }
    }

    class Boxing extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Boxing(TypeConcrete type) {
            super(String.format("Boxing conversion on {%s}.", type),
                    type);
        }

        @Override
        protected TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(), TypePrimitive.class))
                    .value(() -> this.type().typeSystem().box(this.type()))
                    .build();
        }
    }

    class Unboxing extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Unboxing(TypeConcrete type) {
            super(String.format("Unboxing conversion on {%s}.", type),
                    type);
        }

        @Override
        protected TypeResult<TypeConcrete> result() {
            TypeSystem system = this.type().typeSystem();

            return TypeResult.builder(this)
                    .or(new BooleanTypeOperation.Equal(this.type(), system.BOOLEAN_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.BYTE_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.SHORT_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.CHAR_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.INT_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.LONG_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.FLOAT_BOX))
                    .or(new BooleanTypeOperation.Equal(this.type(), system.DOUBLE_BOX))
                    .value(() -> system.unbox(this.type()).orElse(null))
                    .build();
        }
    }

    class Unchecked extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Unchecked(TypeConcrete from, TypeConcrete to) {
            super(String.format("Unchecked conversion from {%s} to {%s}.", from, to),
                    from, to);
        }

        @Override
        protected TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(0), TypeParameterized.class))
                    .and(new BooleanTypeOperation.Kind(this.type(1), TypeParameterized.class))
                    .and(() -> new BooleanTypeOperation.Equal(this.<TypeParameterized>type(0).declaration(), this.<TypeParameterized>type(1).declaration()))
                    .and(() -> this.<TypeParameterized>type(0).arguments().isEmpty())
                    .value(() -> this.<TypeParameterized>type(1))
                    .build();
        }
    }

    class Capture extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Capture(TypeConcrete type) {
            super(String.format("Capture conversion on {%s}.", type),
                    type);
        }

        @Override
        protected TypeResult<TypeConcrete> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(), TypeParameterized.class))
                    .value(() -> {
                        TypeParameterized type = this.type();
                        TypeSystem system = type.typeSystem();
                        List<TypeConcrete> capturedArgs = type.arguments().stream()
                                .map(t -> t instanceof TypeOut || t instanceof TypeIn ? new TypeVariable(system) : t).toList();

                        for (int i = 0; i < type.arguments().size(); i++) {
                            TypeConcrete arg = type.arguments().get(i);
                            if (arg instanceof TypeOut || arg instanceof TypeIn) {
                                TypeVariable fresh = (TypeVariable) capturedArgs.get(i);
                                TypeConcrete ui = type.declaration().parameters().get(i).bound();
                                TypeConcrete substitution = ui.map(t -> {
                                    if (t instanceof TypeParameter param && type.declaration().parameters().contains(param)) {
                                        int index = type.declaration().parameters().indexOf(param);
                                        return capturedArgs.get(index);
                                    }
                                    return t;
                                });

                                if (arg instanceof TypeOut out) { //? extends T
                                    Set<TypeConcrete> types = new LinkedHashSet<>();
                                    types.add(out.bound());
                                    types.add(substitution);

                                    TypeAnd and = new TypeAnd(system, types);
                                    and.lock();
                                    fresh.setUpperBound(and);
                                } else if (arg instanceof TypeIn in) { //? super T
                                    fresh.setUpperBound(substitution);
                                    fresh.setLowerBound(in.bound());
                                }
                                fresh.lock();
                            }
                        }

                        TypeParameterized res = new TypeParameterized(system, type.declaration(),
                                capturedArgs.stream().map(TypeConcrete::flatten).toList());
                        res.lock();
                        return res;
                    })
                    .build();
        }
    }
}
