package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.List;
import java.util.Optional;
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
    public ParameterizedClassType parameterized(List<ArgumentType> typeArguments) {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterized(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterized(ArgumentType... typeArguments) {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterized(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterizedWithTypeVars() {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterizedWithTypeVars());
    }

    @Override
    public ParameterizedClassType parameterizedWithMetaVars() {
        return new ParameterizedClassTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterizedWithMetaVars());
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
    public ParameterizedClassType directSupertype(ClassType supertypeInstance) {
        return this.delegate().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<ClassType> relativeSupertype(ClassType superType) {
        return this.delegate().relativeSupertype(superType);
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

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        return (K) new ClassReferenceDelegate(this.typeSystem(), t -> this.delegate().copy(cache));
    }
}
