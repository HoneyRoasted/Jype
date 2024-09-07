package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class JMethodReferenceImpl extends JAbstractPossiblyUnmodifiableType implements JMethodReference {
    private JMethodLocation location;
    private int modifiers;
    private JClassReference outerClass;
    private JType returnType;
    private List<JType> exceptionTypes = new ArrayList<>();
    private List<JType> parameters = new ArrayList<>();
    private List<JVarType> typeParameters = new ArrayList<>();

    public JMethodReferenceImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JMethodReference copy = this.typeSystem().typeFactory().newMethodReference();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setLocation(this.location);
        copy.setModifiers(this.modifiers);
        copy.setOuterClass(this.outerClass.copy(cache));
        copy.setReturnType(this.returnType.copy(cache));
        copy.setExceptionTypes(this.exceptionTypes.stream().map(t -> (JType) t.copy(cache)).toList());
        copy.setParameters(this.parameters.stream().map(t -> (JType) t.copy(cache)).toList());
        copy.setTypeParameters(this.typeParameters.stream().map(t -> (JVarType) t.copy(cache)).toList());
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public JParameterizedMethodType parameterized(List<JArgumentType> typeArguments) {
        JParameterizedMethodType parameterizedMethodType = this.typeSystem().typeFactory().newParameterizedMethodType();
        parameterizedMethodType.setMethodReference(this);
        parameterizedMethodType.setTypeArguments(typeArguments);
        parameterizedMethodType.setUnmodifiable(true);
        return parameterizedMethodType;
    }

    @Override
    public JParameterizedMethodType parameterized(JArgumentType... typeArguments) {
        return parameterized(List.of(typeArguments));
    }

    @Override
    public JParameterizedMethodType parameterizedWithTypeVars() {
        return this.parameterized((List<JArgumentType>) (List) this.typeParameters);
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

    public JMethodLocation location() {
        return this.location;
    }

    public void setLocation(JMethodLocation location) {
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
    public JClassReference outerClass() {
        return this.outerClass;
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        this.checkUnmodifiable();
        this.outerClass = outerClass;
    }

    public JType returnType() {
        return this.returnType;
    }

    public void setReturnType(JType returnType) {
        super.checkUnmodifiable();
        this.returnType = returnType;
    }

    @Override
    public List<JType> exceptionTypes() {
        return this.exceptionTypes;
    }

    @Override
    public void setExceptionTypes(List<JType> exceptionTypes) {
        this.checkUnmodifiable();
        ;
        this.exceptionTypes = exceptionTypes;
    }

    public List<JVarType> typeParameters() {
        return this.typeParameters;
    }

    public void setTypeParameters(List<JVarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    public List<JType> parameters() {
        return this.parameters;
    }

    public void setParameters(List<JType> parameters) {
        super.checkUnmodifiable();
        this.parameters = parameters;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JMethodType mt) {
            if (Objects.equals(location, mt.location()) && modifiers == mt.modifiers() && JType.equals(returnType, mt.returnType(), kind, seen) &&
                    JType.equals(exceptionTypes, mt.exceptionTypes(), kind, seen) && JType.equals(parameters, mt.parameters(), kind, seen) && JType.equals(typeParameters, mt.typeParameters(), kind, seen) &&
                    ((!this.hasRelevantOuterType() && !mt.hasRelevantOuterType()) || JType.equals(this.outerClass, mt.outerType(), kind, seen))) {
                if (mt instanceof JParameterizedMethodType pmt) {
                    return (!pmt.hasRelevantOuterType() || JType.equals(pmt.typeArguments(), typeParameters, kind, seen));
                } else {
                    return true;
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

        return JType.multiHash(Objects.hashCode(location), modifiers, JType.hashCode(outerClass, seen),
                JType.hashCode(returnType, seen), JType.hashCode(exceptionTypes, seen), JType.hashCode(parameters, seen),
                JType.hashCode(typeParameters, seen));
    }

}
