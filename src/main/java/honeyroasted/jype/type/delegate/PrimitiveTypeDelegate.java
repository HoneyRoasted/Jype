package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class PrimitiveTypeDelegate extends AbstractTypeDelegate<PrimitiveType> implements PrimitiveType {

    public PrimitiveTypeDelegate(TypeSystem system, Function<TypeSystem, PrimitiveType> factory) {
        super(system, factory);
    }

    @Override
    public ClassNamespace namespace() {
        return this.delegate().namespace();
    }

    @Override
    public ClassReference box() {
        return this.delegate().box();
    }

    @Override
    public String name() {
        return this.delegate().name();
    }

    @Override
    public String descriptor() {
        return this.delegate().descriptor();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new PrimitiveTypeDelegate(this.typeSystem(), DelegateType.delayAndCache(t -> this.delegate().copy(cache)));
    }
}
