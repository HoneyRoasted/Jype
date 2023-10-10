package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.MetaVarType;

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

}
