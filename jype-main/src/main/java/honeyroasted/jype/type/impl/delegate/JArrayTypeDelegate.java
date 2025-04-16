package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public class JArrayTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JArrayType> implements JArrayType {

    public JArrayTypeDelegate(JTypeSystem system, Function<JTypeSystem, JArrayType> factory) {
        super(system, factory, JArrayTypeDelegate::new);
    }

    @Override
    public JType component() {
        return delegate().component();
    }

    @Override
    public void setComponent(JType component) {
        delegate().setComponent(component);
    }

    @Override
    public int depth() {
        return delegate().depth();
    }
}
