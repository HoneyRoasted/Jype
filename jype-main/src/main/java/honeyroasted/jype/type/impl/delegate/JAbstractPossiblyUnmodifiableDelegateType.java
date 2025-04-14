package honeyroasted.jype.type.impl.delegate;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public class JAbstractPossiblyUnmodifiableDelegateType<T extends JType & PossiblyUnmodifiable> extends JAbstractDelegateType<T> implements PossiblyUnmodifiable {

    public JAbstractPossiblyUnmodifiableDelegateType(JTypeSystem system, Function<JTypeSystem, T> factory) {
        super(system, factory);
    }

    @Override
    public boolean isUnmodifiable() {
        return delegate().isUnmodifiable();
    }

    @Override
    public void setUnmodifiable(boolean b) {
        delegate().setUnmodifiable(b);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return delegate().equals(o);
    }
}
