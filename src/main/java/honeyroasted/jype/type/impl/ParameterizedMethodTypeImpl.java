package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ParameterizedMethodTypeImpl extends AbstractPossiblyUnmodifiableType implements ParameterizedMethodType {
    private MethodReference methodReference;
    private ClassType outerType;
    private List<ArgumentType> typeArguments;

    public ParameterizedMethodTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        ParameterizedMethodType copy = new ParameterizedMethodTypeImpl(this.typeSystem());
        cache.put(this, copy);

        copy.setMethodReference(this.methodReference.copy(cache));
        copy.setOuterType(this.outerType.copy(cache));
        copy.setTypeArguments(this.typeArguments.stream().map(t -> (ArgumentType) t.copy(cache)).toList());
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeArguments = List.copyOf(this.typeArguments);
    }

    @Override
    protected void makeModifiable() {
        this.typeArguments = new ArrayList<>(this.typeArguments);
    }

    public MethodReference methodReference() {
        return this.methodReference;
    }

    @Override
    public ClassType outerType() {
        return this.outerType;
    }

    @Override
    public void setOuterType(ClassType outerType) {
        this.outerType = outerType;
    }

    @Override
    public void setMethodReference(MethodReference methodReference) {
        super.checkUnmodifiable();
        this.methodReference = methodReference;
    }

    @Override
    public List<ArgumentType> typeArguments() {
        return this.typeArguments;
    }

    @Override
    public void setTypeArguments(List<ArgumentType> typeArguments) {
        super.checkUnmodifiable();
        this.typeArguments = typeArguments;
    }

    @Override
    public MethodLocation location() {
        return methodReference.location();
    }

    @Override
    public void setLocation(MethodLocation location) {
        methodReference.setLocation(location);
    }

    @Override
    public int modifiers() {
        return methodReference.modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        methodReference.setModifiers(modifiers);
    }

    @Override
    public ClassReference outerClass() {
        return methodReference.outerClass();
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        methodReference.setOuterClass(outerClass);
    }

    @Override
    public Type returnType() {
        return methodReference.returnType();
    }

    @Override
    public void setReturnType(Type returnType) {
        methodReference.setReturnType(returnType);
    }

    @Override
    public List<Type> exceptionTypes() {
        return this.methodReference.exceptionTypes();
    }

    @Override
    public void setExceptionTypes(List<Type> exceptionTypes) {
        this.methodReference.setExceptionTypes(exceptionTypes);
    }

    @Override
    public List<VarType> typeParameters() {
        return methodReference.typeParameters();
    }

    @Override
    public void setTypeParameters(List<VarType> typeParameters) {
        methodReference.setTypeParameters(typeParameters);
    }

    @Override
    public List<Type> parameters() {
        return methodReference.parameters();
    }

    @Override
    public void setParameters(List<Type> parameters) {
        methodReference.setParameters(parameters);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof MethodType)) return false;
        if (o instanceof ParameterizedMethodType pt) {
            return Objects.equals(methodReference, pt.methodReference()) && Objects.equals(typeArguments, pt.typeArguments()) && (Modifier.isStatic(this.modifiers()) || Objects.equals(outerType, pt.outerType()));
        } else if (o instanceof MethodReference mr) {
            return Objects.equals(methodReference, mr) && (typeArguments.isEmpty() || Objects.equals(typeArguments, mr.typeParameters())) && (Modifier.isStatic(this.modifiers()) || Objects.equals(outerType, mr.outerClass()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodReference, typeArguments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.methodReference);
        if (!this.typeArguments().isEmpty()) {
            sb.append("<");
            for (int i = 0; i < this.typeArguments().size(); i++) {
                sb.append(this.typeArguments().get(i));
                if (i < this.typeArguments().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
