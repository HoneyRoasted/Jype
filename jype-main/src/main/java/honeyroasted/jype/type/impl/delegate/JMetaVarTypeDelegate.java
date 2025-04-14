package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.Set;
import java.util.function.Function;

public class JMetaVarTypeDelegate extends JAbstractDelegateType<JMetaVarType> implements JMetaVarType {
    public JMetaVarTypeDelegate(JTypeSystem system, Function<JTypeSystem, JMetaVarType> factory) {
        super(system, factory);
    }

    @Override
    public int identity() {
        return delegate().identity();
    }

    @Override
    public String name() {
        return delegate().name();
    }

    @Override
    public Set<JType> upperBounds() {
        return delegate().upperBounds();
    }

    @Override
    public Set<JType> lowerBounds() {
        return delegate().lowerBounds();
    }

    @Override
    public Set<JType> equalities() {
        return delegate().equalities();
    }
}
