package honeyroasted.jype.system.operations;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.operations.result.TypeResult;
import honeyroasted.jype.type.TypePrimitive;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface TypeDirectSuper extends TypeOperation<Set<TypeConcrete>> {
    Map<String, Set<String>> PRIMITIVE_DIRECT_SUPERS =
            Map.of(
                    "Z", Set.of(),
                    "B", Set.of("S"),
                    "S", Set.of("I"),
                    "C", Set.of("I"),
                    "I", Set.of("J"),
                    "J", Set.of("F"),
                    "F", Set.of("D"),
                    "D", Set.of()
            );

    class Primitive extends AbstractTypeOperation<Set<TypeConcrete>> implements TypeDirectSuper {

        public Primitive(TypeConcrete type) {
            super(String.format("Direct primitive supertype of {%s}.", type),
                    type);
        }

        @Override
        protected TypeResult<Set<TypeConcrete>> result() {
            return TypeResult.builder(this)
                    .and(new BooleanTypeOperation.Kind(this.type(), TypePrimitive.class))
                    .value(() -> {
                        TypePrimitive type = this.type();
                        return PRIMITIVE_DIRECT_SUPERS.get(type.descriptor()).stream().map(type.typeSystem().DESCRIPTOR_TO_PRIMITIVE::get)
                                .collect(Collectors.toUnmodifiableSet());
                    })
                    .build();
        }
    }

}
