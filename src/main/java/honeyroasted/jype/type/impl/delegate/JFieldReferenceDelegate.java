package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.metadata.location.JFieldLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public class JFieldReferenceDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JFieldReference> implements JFieldReference {
    public JFieldReferenceDelegate(JTypeSystem system, Function<JTypeSystem, JFieldReference> factory) {
        super(system, factory);
    }

    @Override
    public JFieldLocation location() {
        return delegate().location();
    }

    @Override
    public void setLocation(JFieldLocation location) {
        delegate().setLocation(location);
    }

    @Override
    public JClassReference outerClass() {
        return delegate().outerClass();
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        delegate().setOuterClass(outerClass);
    }

    @Override
    public JType type() {
        return delegate().type();
    }

    @Override
    public void setType(JType type) {
        delegate().setType(type);
    }

    @Override
    public int modifiers() {
        return delegate().modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        delegate().setModifiers(modifiers);
    }
}
