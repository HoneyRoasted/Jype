package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.util.Set;
import java.util.function.Function;

public class JWildTypeUpperDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JWildType.Upper> implements JWildType.Upper {

    public JWildTypeUpperDelegate(JTypeSystem system, Function<JTypeSystem, Upper> factory) {
        super(system, factory, JWildTypeUpperDelegate::new);
    }

    @Override
    public int identity() {
        return delegate().identity();
    }

    @Override
    public void setIdentity(int identity) {
        delegate().setIdentity(identity);
    }

    @Override
    public Set<JType> upperBounds() {
        return delegate().upperBounds();
    }

    @Override
    public void setUpperBounds(Set<JType> upperBounds) {
        delegate().setUpperBounds(upperBounds);
    }

    @Override
    public Set<JType> lowerBounds() {
        return delegate().lowerBounds();
    }

    @Override
    public void setLowerBounds(Set<JType> lowerBounds) {
        delegate().setLowerBounds(lowerBounds);
    }
}
