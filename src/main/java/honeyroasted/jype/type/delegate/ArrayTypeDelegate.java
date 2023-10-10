package honeyroasted.jype.type.delegate;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public class ArrayTypeDelegate extends AbstractTypeDelegate<ArrayType> implements ArrayType {

    public ArrayTypeDelegate(TypeSystem system, Function<TypeSystem, ArrayType> factory) {
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
    public Type component() {
        return this.delegate().component();
    }

    @Override
    public void setComponent(Type component) {
        this.delegate().setComponent(component);
    }

    @Override
    public int depth() {
        return this.delegate().depth();
    }

}
