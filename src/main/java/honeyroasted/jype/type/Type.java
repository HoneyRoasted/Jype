package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

public interface Type {

    TypeSystem typeSystem();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

    default <T extends Type> TypeWithMetadata<T> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>((T) this, metadata);
    }

    default Type stripMetadata() {
        return this;
    }

}
