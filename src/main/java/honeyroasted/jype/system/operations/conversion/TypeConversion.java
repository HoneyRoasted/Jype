package honeyroasted.jype.system.operations.conversion;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.operations.AbstractTypeOperation;
import honeyroasted.jype.system.operations.BooleanTypeOperation;
import honeyroasted.jype.system.operations.TypeOperation;
import honeyroasted.jype.system.operations.result.TypeResult;
import honeyroasted.jype.type.TypePrimitive;

import java.util.Map;
import java.util.Set;

public interface TypeConversion extends TypeOperation<TypeConcrete> {

    class Identity extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        public Identity(TypeConcrete from, TypeConcrete to) {
            super(String.format("Identity conversion from {%s} to {%s}", from, to),
                    from, to);
        }

        @Override
        public TypeResult<TypeConcrete> perform() {
            return TypeResult.builder(this)
                    .prerequisites(new BooleanTypeOperation.Equal(this.type(0), this.type(1)))
                    .build();
        }
    }

    class WideningPrimitive extends AbstractTypeOperation<TypeConcrete> implements TypeConversion {
        private static final Map<String, Set<String>> PRIM_SUPERS = Map.of(
                "Z", Set.of(),
                "B", Set.of("S", "C", "I", "J", "F", "D"),
                "S", Set.of("C", "I", "J", "F", "D"),
                "C", Set.of("S", "I", "J", "F", "D"),
                "I", Set.of("J", "F", "D"),
                "J", Set.of("F", "D"),
                "F", Set.of("D"),
                "D", Set.of()
        );

        public WideningPrimitive(TypeConcrete from, TypeConcrete to) {
            super(String.format("Widening primitive conversion from {%s} to {%s}", from, to),
                    from, to);
        }

        @Override
        public TypeResult<TypeConcrete> perform() {
            return TypeResult.builder(this)
                    .prerequisites(
                            new BooleanTypeOperation.Kind(this.type(0), TypePrimitive.class),
                            new BooleanTypeOperation.Kind(this.type(1), TypePrimitive.class)
                    )
                    .and(() -> {
                        TypePrimitive from = this.type(0);
                        TypePrimitive to = this.type(1);
                        return PRIM_SUPERS.get(from.descriptor()).contains(to.descriptor());
                    })
                    .value(() -> this.type(1))
                    .build();
        }
    }

}
