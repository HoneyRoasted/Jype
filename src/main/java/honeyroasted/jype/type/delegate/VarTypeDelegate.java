package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Set;
import java.util.function.Function;

public class VarTypeDelegate extends AbstractTypeDelegate<VarType> implements VarType {

    public VarTypeDelegate(TypeSystem system, Function<TypeSystem, VarType> factory) {
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
    public TypeParameterLocation location() {
        return this.delegate().location();
    }

    @Override
    public void setLocation(TypeParameterLocation location) {
        this.delegate().setLocation(location);
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
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new VarTypeDelegate(this.typeSystem(), t -> this.delegate().copy(cache));
    }
}
