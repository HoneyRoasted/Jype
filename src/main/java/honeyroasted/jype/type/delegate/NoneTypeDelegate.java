package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.NoneType;

import java.util.function.Function;

public class NoneTypeDelegate extends AbstractTypeDelegate<NoneType> implements NoneType {

    public NoneTypeDelegate(TypeSystem system, Function<TypeSystem, NoneType> factory) {
        super(system, factory);
    }

    @Override
    public String name() {
        return this.delegate().name();
    }
}
