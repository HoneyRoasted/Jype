package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.*;

import java.util.List;
import java.util.function.Function;

public class ClassReferenceDelegate extends AbstractTypeDelegate<ClassReference> implements ClassReference {

    public ClassReferenceDelegate(TypeSystem system, Function<TypeSystem, ClassReference> factory) {
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
    public ParameterizedClassType parameterized(List<Type> typeArguments) {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterized(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterized(Type... typeArguments) {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterized(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterizedWithTypeVars() {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterizedWithTypeVars());
    }

    @Override
    public ClassReference outerClass() {
        return this.delegate().outerClass();
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        this.delegate().setOuterClass(outerClass);
    }

    @Override
    public int modifiers() {
        return this.delegate().modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        this.delegate().setModifiers(modifiers);
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
}
