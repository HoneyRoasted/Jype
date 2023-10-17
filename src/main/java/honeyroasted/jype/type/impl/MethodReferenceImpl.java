package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MethodReferenceImpl extends AbstractPossiblyUnmodifiableType implements MethodReference {
    private MethodLocation location;
    private int modifiers;
    private ClassReference outerClass;
    private Type returnType;
    private List<Type> exceptionTypes = new ArrayList<>();
    private List<Type> parameters = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public MethodReferenceImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public String simpleName() {
        return this.location.simpleName() + "(" +
                this.parameters.stream().map(Type::simpleName).collect(Collectors.joining(", ")) + ") -> " +
                this.returnType.simpleName();
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        MethodReference copy = new MethodReferenceImpl(this.typeSystem());
        cache.put(this, copy);

        copy.setLocation(this.location);
        copy.setModifiers(this.modifiers);
        copy.setOuterClass(this.outerClass.copy(cache));
        copy.setReturnType(this.returnType.copy(cache));
        copy.setExceptionTypes(this.exceptionTypes.stream().map(t -> (Type) t.copy(cache)).toList());
        copy.setParameters(this.parameters.stream().map(t -> (Type) t.copy(cache)).toList());
        copy.setTypeParameters(this.typeParameters.stream().map(t -> (VarType) t.copy(cache)).toList());
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public ParameterizedMethodType asMethodType(List<ArgumentType> typeArguments) {
        ParameterizedMethodType parameterizedMethodType = new ParameterizedMethodTypeImpl(this.typeSystem());
        parameterizedMethodType.setMethodReference(this);
        parameterizedMethodType.setTypeArguments(typeArguments);
        parameterizedMethodType.setUnmodifiable(true);
        return parameterizedMethodType;
    }

    @Override
    public ParameterizedMethodType asMethodType(ArgumentType... typeArguments) {
        return asMethodType(List.of(typeArguments));
    }

    @Override
    public ParameterizedMethodType parameterizedWithTypeVars() {
        return this.asMethodType((List<ArgumentType>) (List) this.typeParameters);
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeParameters = List.copyOf(this.typeParameters);
        this.parameters = List.copyOf(this.parameters);
        this.exceptionTypes = List.copyOf(this.exceptionTypes);
    }

    @Override
    protected void makeModifiable() {
        this.typeParameters = new ArrayList<>(this.typeParameters);
        this.parameters = new ArrayList<>(this.parameters);
        this.exceptionTypes = new ArrayList<>(this.exceptionTypes);
    }

    public MethodLocation location() {
        return this.location;
    }

    public void setLocation(MethodLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public int modifiers() {
        return this.modifiers;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.checkUnmodifiable();
        this.modifiers = modifiers;
    }

    @Override
    public ClassReference outerClass() {
        return this.outerClass;
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        this.checkUnmodifiable();
        this.outerClass = outerClass;
    }

    public Type returnType() {
        return this.returnType;
    }

    public void setReturnType(Type returnType) {
        super.checkUnmodifiable();
        this.returnType = returnType;
    }

    @Override
    public List<Type> exceptionTypes() {
        return this.exceptionTypes;
    }

    @Override
    public void setExceptionTypes(List<Type> exceptionTypes) {
        this.checkUnmodifiable();
        ;
        this.exceptionTypes = exceptionTypes;
    }

    public List<VarType> typeParameters() {
        return this.typeParameters;
    }

    public void setTypeParameters(List<VarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    public List<Type> parameters() {
        return this.parameters;
    }

    public void setParameters(List<Type> parameters) {
        super.checkUnmodifiable();
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntersectionType it) {
            return it.equals(this);
        }

        if (this == o) return true;
        if (o == null || !(o instanceof MethodType)) return false;
        if (o instanceof MethodReference mr) {
            return Objects.equals(location, mr.location()) && Objects.equals(returnType, mr.returnType()) && Objects.equals(outerClass, mr.outerClass()) && Objects.equals(parameters, mr.parameters()) && Objects.equals(typeParameters, mr.typeParameters()) && modifiers == mr.modifiers();
        } else if (o instanceof ParameterizedMethodType pt) {
            return pt.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, returnType, outerClass, parameters, typeParameters, exceptionTypes);
    }

}
