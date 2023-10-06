package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Set;

public interface ArrayType extends PossiblyUnmodifiable, Type {
    Type component();

    void setComponent(Type component);

    int depth();

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.component().hasCyclicTypeVariables();
    }

    @Override
    default TypeWithMetadata<ArrayType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }

    @Override
    default  <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitArrayType(this, context);
    }
}
