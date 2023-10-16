package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public abstract class AbstractTypeDelegate<T extends Type> implements DelegateType<T> {
    private TypeSystem system;
    private T delegate;
    private Function<TypeSystem, T> factory;

    public AbstractTypeDelegate(TypeSystem system, Function<TypeSystem, T> factory) {
        this.factory = factory;
        this.system = system;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.system;
    }

    public T delegate() {
        if (this.delegate == null) {
            this.delegate = this.factory.apply(this.system);
        }
        return delegate;
    }

    public void expireDelegate() {
        this.delegate = null;
    }

    @Override
    public String simpleName() {
        return this.delegate.simpleName();
    }

    @Override
    public int hashCode() {
        return this.delegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.delegate().equals(obj);
    }

    @Override
    public String toString() {
        return this.delegate().toString();
    }
}
