package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class NoneTypeDelegate extends AbstractTypeDelegate<NoneType> implements NoneType {

    public NoneTypeDelegate(TypeSystem system, Function<TypeSystem, NoneType> factory) {
        super(system, factory);
    }

    @Override
    public String name() {
        return this.delegate().name();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new NoneTypeDelegate(this.typeSystem(), DelegateType.delayAndCache(t -> this.delegate().copy(cache)));
    }
}
