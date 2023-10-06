package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ParameterizedClassTypeDelegate extends AbstractTypeDelegate<ParameterizedClassType> implements ParameterizedClassType {

    public ParameterizedClassTypeDelegate(TypeSystem system, Function<TypeSystem, ParameterizedClassType> factory) {
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
    public ClassNamespace namespace() {
        return this.delegate().namespace();
    }

    @Override
    public void setNamespace(ClassNamespace namespace) {
        this.delegate().setNamespace(namespace);
    }

    @Override
    public boolean isInterface() {
        return this.delegate().isInterface();
    }

    @Override
    public void setInterface(boolean isInterface) {
        this.delegate().setInterface(isInterface);
    }

    @Override
    public ClassType superClass() {
        return this.delegate().superClass();
    }

    @Override
    public void setSuperClass(ClassType superClass) {
        this.delegate().setSuperClass(superClass);
    }

    @Override
    public List<ClassType> interfaces() {
        return this.delegate().interfaces();
    }

    @Override
    public void setInterfaces(List<ClassType> interfaces) {
        this.delegate().setInterfaces(interfaces);
    }

    @Override
    public List<VarType> typeParameters() {
        return this.delegate().typeParameters();
    }

    @Override
    public void setTypeParameters(List<VarType> typeParameters) {
        this.delegate().setTypeParameters(typeParameters);
    }

    @Override
    public boolean hasSupertype(ClassReference supertype) {
        return this.delegate().hasSupertype(supertype);
    }

    @Override
    public ClassReference classReference() {
        return this.delegate().classReference();
    }

    @Override
    public List<Type> typeArguments() {
        return this.delegate().typeArguments();
    }

    @Override
    public void setTypeArguments(List<Type> typeArguments) {
        this.delegate().setTypeArguments(typeArguments);
    }

    @Override
    public void setClassReference(ClassReference classReference) {
        this.delegate().setClassReference(classReference);
    }

    @Override
    public ParameterizedClassType directSupertype(ClassType supertypeInstance) {
        return this.delegate().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<ClassType> relativeSupertype(ClassReference superType) {
        return this.delegate().relativeSupertype(superType);
    }
}
