package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JParameterizedMethodTypeImpl extends JAbstractPossiblyUnmodifiableType implements JParameterizedMethodType {
    private JMethodReference methodReference;
    private JClassType outerType;
    private List<JArgumentType> typeArguments;

    public JParameterizedMethodTypeImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JParameterizedMethodType copy = this.typeSystem().typeFactory().newParameterizedMethodType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setMethodReference(this.methodReference.copy(cache));
        copy.setOuterType(this.outerType.copy(cache));
        copy.setTypeArguments(this.typeArguments.stream().map(t -> (JArgumentType) t.copy(cache)).toList());
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

    public JMethodReference methodReference() {
        return this.methodReference;
    }

    @Override
    public JClassType outerType() {
        return this.outerType;
    }

    @Override
    public void setOuterType(JClassType outerType) {
        this.outerType = outerType;
    }

    @Override
    public void setMethodReference(JMethodReference methodReference) {
        super.checkUnmodifiable();
        this.methodReference = methodReference;
    }

    @Override
    public List<JArgumentType> typeArguments() {
        return this.typeArguments;
    }

    @Override
    public void setTypeArguments(List<JArgumentType> typeArguments) {
        super.checkUnmodifiable();
        this.typeArguments = typeArguments;
    }

    @Override
    public JMethodLocation location() {
        return methodReference.location();
    }

    @Override
    public void setLocation(JMethodLocation location) {
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
    public JClassReference outerClass() {
        return methodReference.outerClass();
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        methodReference.setOuterClass(outerClass);
    }

    @Override
    public JType returnType() {
        return methodReference.returnType();
    }

    @Override
    public void setReturnType(JType returnType) {
        methodReference.setReturnType(returnType);
    }

    @Override
    public List<JType> exceptionTypes() {
        return this.methodReference.exceptionTypes();
    }

    @Override
    public void setExceptionTypes(List<JType> exceptionTypes) {
        this.methodReference.setExceptionTypes(exceptionTypes);
    }

    @Override
    public List<JVarType> typeParameters() {
        return methodReference.typeParameters();
    }

    @Override
    public void setTypeParameters(List<JVarType> typeParameters) {
        methodReference.setTypeParameters(typeParameters);
    }

    @Override
    public List<JType> parameters() {
        return methodReference.parameters();
    }

    @Override
    public void setParameters(List<JType> parameters) {
        methodReference.setParameters(parameters);
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JMethodType mt) {
            if (JType.equals(methodReference, mt.methodReference(), kind, seen)) {
                if (mt instanceof JMethodReference mr) {
                    return !this.hasTypeArguments() || (JType.equals(typeArguments, mr.typeParameters(), kind, seen));
                } else if (mt instanceof JParameterizedMethodType pmt) {
                    return JType.equals(typeArguments, pmt.typeArguments(), kind, seen) &&
                            ((!this.hasRelevantOuterType() && !pmt.hasRelevantOuterType()) || JType.equals(outerType, pmt.outerType(), kind, seen));
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
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        seen = JType.concat(seen, this);

        if (JType.structuralEquals(this, this.methodReference)) {
            return JType.hashCode(methodReference, seen);
        } else {
            return JType.multiHash(JType.hashCode(methodReference, seen), JType.hashCode(outerType, seen),
                    JType.hashCode(typeArguments, seen));
        }
    }

}
