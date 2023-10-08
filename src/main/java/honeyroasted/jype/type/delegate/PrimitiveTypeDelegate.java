package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.PrimitiveType;

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
    public ClassNamespace boxNamespace() {
        return this.delegate().boxNamespace();
    }

    @Override
    public String name() {
        return this.delegate().name();
    }

    @Override
    public String descriptor() {
        return this.delegate().descriptor();
    }
}
