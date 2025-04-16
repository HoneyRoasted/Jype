package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Set;
import java.util.function.Function;

public class JVarTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JVarType> implements JVarType {
    public JVarTypeDelegate(JTypeSystem system, Function<JTypeSystem, JVarType> factory) {
        super(system, factory, JVarTypeDelegate::new);
    }

    @Override
    public JTypeParameterLocation location() {
        return delegate().location();
    }

    @Override
    public void setLocation(JTypeParameterLocation location) {
        delegate().setLocation(location);
    }

    @Override
    public Set<JType> upperBounds() {
        return delegate().upperBounds();
    }

    @Override
    public void setUpperBounds(Set<JType> upperBounds) {
        delegate().setUpperBounds(upperBounds);
    }
}
