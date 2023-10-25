package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Set;
import java.util.function.Function;

public class MetaVarTypeDelegate extends AbstractTypeDelegate<MetaVarType> implements MetaVarType {

    public MetaVarTypeDelegate(TypeSystem system, Function<TypeSystem, MetaVarType> factory) {
        super(system, factory);
    }

    @Override
    public int identity() {
        return this.delegate().identity();
    }

    @Override
    public String name() {
        return this.delegate().name();
    }

    @Override
    public Set<Type> upperBounds() {
        return this.delegate().upperBounds();
    }

    @Override
    public Set<Type> lowerBounds() {
        return this.delegate().lowerBounds();
    }

    @Override
    public Set<Type> equalities() {
        return this.delegate().equalities();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new MetaVarTypeDelegate(this.typeSystem(), t -> this.delegate().copy(cache));
    }

}
