package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.Set;
import java.util.function.Function;

public class IntersectionTypeDelegate extends AbstractTypeDelegate<IntersectionType> implements IntersectionType {

    public IntersectionTypeDelegate(TypeSystem system, Function<TypeSystem, IntersectionType> factory) {
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
    public Set<Type> children() {
        return this.delegate().children();
    }

    @Override
    public void setChildren(Set<Type> children) {
        this.delegate().setChildren(children);
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new IntersectionTypeDelegate(this.typeSystem(), DelegateType.delayAndCache(t -> this.delegate().copy(cache)));
    }
}
