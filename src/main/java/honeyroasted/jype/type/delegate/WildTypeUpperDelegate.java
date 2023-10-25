package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.Set;
import java.util.function.Function;

public class WildTypeUpperDelegate extends AbstractTypeDelegate<WildType.Upper> implements WildType.Upper {

    public WildTypeUpperDelegate(TypeSystem system, Function<TypeSystem, Upper> factory) {
        super(system, factory);
    }

    @Override
    public boolean isUnmodifiable() {
        return this.delegate().isUnmodifiable();
    }

    @Override
    public void setUnmodifiable(boolean unmodifiable) {
        this.delegate().setUnmodifiable(unmodifiable);
    }

    @Override
    public int identity() {
        return this.delegate().identity();
    }

    @Override
    public void setIdentity(int identity) {
        this.delegate().setIdentity(identity);
    }

    @Override
    public Set<Type> upperBounds() {
        return this.delegate().upperBounds();
    }

    @Override
    public void setUpperBounds(Set<Type> upperBounds) {
        this.delegate().setUpperBounds(upperBounds);
    }

    @Override
    public Set<Type> lowerBounds() {
        return this.delegate().lowerBounds();
    }

    @Override
    public void setLowerBounds(Set<Type> lowerBounds) {
        this.delegate().setLowerBounds(lowerBounds);
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new WildTypeUpperDelegate(this.typeSystem(), t -> this.delegate().copy(cache));
    }
}
