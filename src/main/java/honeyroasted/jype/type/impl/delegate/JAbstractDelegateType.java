package honeyroasted.jype.type.impl.delegate;

import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JType;

import java.util.Set;
import java.util.function.Function;

public class JAbstractDelegateType<T extends JType> implements JType {
    private JTypeSystem typeSystem;
    private Function<JTypeSystem, T> factory;
    private T delegate;

    public JAbstractDelegateType(JTypeSystem system, Function<JTypeSystem, T> factory) {
        this.typeSystem = system;
        this.factory = factory;
    }

    protected T delegate() {
        if (this.delegate == null) this.delegate = factory.apply(this.typeSystem);
        return this.delegate;
    }

    @Override
    public JTypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public String simpleName() {
        return delegate().simpleName();
    }

    @Override
    public <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return delegate().accept(visitor, context);
    }

    @Override
    public PropertySet metadata() {
        return delegate().metadata();
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        return delegate().equals(other, kind, seen);
    }

    @Override
    public int hashCode(Set<JType> seen) {
        return delegate.hashCode(seen);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        return delegate().copy(cache);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return delegate().equals(o);
    }
}
