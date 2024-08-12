package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.Pair;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        ParameterizedMethodType copy = this.typeSystem().typeFactory().newParameterizedMethodType();
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

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        seen = Type.concat(seen, Pair.identity(this, other));

        if (other instanceof MethodType mt) {
            if (Type.equals(methodReference, mt.methodReference(), kind, seen)) {
                if (mt instanceof MethodReference mr) {
                    return !this.hasTypeArguments() || (Type.equals(typeArguments, mr.typeParameters(), kind, seen));
                } else if (mt instanceof ParameterizedMethodType pmt) {
                    return Type.equals(typeArguments, pmt.typeArguments(), kind, seen) &&
                            ((!this.hasRelevantOuterType() && !pmt.hasRelevantOuterType()) || Type.equals(outerType, pmt.outerType(), kind, seen));
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        if (Type.structuralEquals(this, this.methodReference)) {
            return Type.hashCode(methodReference, seen);
        } else {
            return Type.multiHash(Type.hashCode(methodReference, seen), Type.hashCode(outerType, seen),
                    Type.hashCode(typeArguments, seen));
        }
    }

}
