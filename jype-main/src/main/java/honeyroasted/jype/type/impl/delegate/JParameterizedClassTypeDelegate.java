package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JParameterizedClassType;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JParameterizedClassTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JParameterizedClassType> implements JParameterizedClassType {

    public JParameterizedClassTypeDelegate(JTypeSystem system, Function<JTypeSystem, JParameterizedClassType> factory) {
        super(system, factory, JParameterizedClassTypeDelegate::new);
    }

    @Override
    public void setClassReference(JClassReference classReference) {
        delegate().setClassReference(classReference);
    }

    @Override
    public JClassType outerType() {
        return delegate().outerType();
    }

    @Override
    public JParameterizedClassType directSupertype(JClassType supertypeInstance) {
        return delegate().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<JClassType> relativeSupertype(JClassType superType) {
        return delegate().relativeSupertype(superType);
    }

    @Override
    public JClassReference classReference() {
        return delegate().classReference();
    }

    @Override
    public List<JArgumentType> typeArguments() {
        return delegate().typeArguments();
    }

    @Override
    public void setTypeArguments(List<JArgumentType> typeArguments) {
        delegate().setTypeArguments(typeArguments);
    }

    @Override
    public void setOuterType(JClassType outerType) {
        delegate().setOuterType(outerType);
    }
}
