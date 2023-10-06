package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.Type;

public final class TypeWithMetadata<T extends Type> implements Type {
    private T type;
    private TypeMetadata metadata;

    public TypeWithMetadata(T type, TypeMetadata metadata) {
        this.type = type;
        this.metadata = metadata;
    }

    public T type() {
        return this.type;
    }

    public void setType(T type) {
        this.type = type;
    }

    public TypeMetadata metadata() {
        return this.metadata;
    }

    public void setMetadata(TypeMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.type.typeSystem();
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return this.type.accept(visitor, context);
    }

    @Override
    public TypeWithMetadata<T> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this.type, metadata);
    }

    @Override
    public T stripMetadata() {
        return this.type;
    }
}
