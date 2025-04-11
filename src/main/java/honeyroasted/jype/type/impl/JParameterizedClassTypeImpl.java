package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JParameterizedClassTypeImpl extends JAbstractPossiblyUnmodifiableType implements JParameterizedClassType {
    private JClassReference classReference;
    private JClassType outerType;
    private List<JArgumentType> typeArguments;

    private final transient Map<JClassType, JClassType> superTypes = new HashMap<>();

    public JParameterizedClassTypeImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JParameterizedClassType copy = this.typeSystem().typeFactory().newParameterizedClassType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setClassReference(this.classReference.copy(cache));
        copy.setOuterType(this.outerType == null ? outerType : outerType.copy(cache));
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
        this.superTypes.clear();
    }

    @Override
    public Optional<JClassType> relativeSupertype(JClassType superType) {
        JClassType result = this.superTypes.get(superType);
        if (result != null) {
            return Optional.of(result);
        }

        Optional<List<JClassType>> pathOpt = this.hierarchyPathTo(superType.classReference());
        if (pathOpt.isPresent()) {
            List<JClassType> path = pathOpt.get();
            if (!path.isEmpty()) {
                Iterator<JClassType> iter = path.iterator();
                JClassType curr = iter.next();
                while (iter.hasNext()) {
                    curr = curr.directSupertype(iter.next());
                }

                if (superType instanceof JParameterizedClassType pct) {
                    curr = (JParameterizedClassType) pct.varTypeResolver().visit(curr.hasTypeArguments() ? curr : curr.classReference().parameterizedWithTypeVars());
                }

                if (!curr.hasTypeArguments()) {
                    curr = curr.classReference();
                }

                this.superTypes.put(superType, curr);
                return Optional.of(curr);
            }
        }

        return Optional.empty();
    }

    public JParameterizedClassType directSupertype(JClassType supertypeInstance) {
        if (supertypeInstance instanceof JParameterizedClassType pType) {
            JVarTypeResolveVisitor varTypeResolver = this.varTypeResolver();
            JParameterizedClassType res = pType.classReference().parameterized(pType.typeArguments().stream().map(t -> (JArgumentType) varTypeResolver.visit(t)).toList());

            if (this.hasRelevantOuterType() && pType.hasRelevantOuterType()) {
                Optional<JClassType> outerRelative = this.outerType().relativeSupertype(pType.outerType());
                if (outerRelative.isPresent()) {
                    res.setUnmodifiable(false);
                    res.setOuterType(outerRelative.get());
                    res.setUnmodifiable(true);
                    return res;
                } else {
                    return null;
                }
            } else {
                return res;
            }
        } else if (supertypeInstance instanceof JClassReference rType) {
            return rType.parameterized();
        }

        return null;
    }

    public JClassReference classReference() {
        return this.classReference;
    }

    public void setClassReference(JClassReference classReference) {
        super.checkUnmodifiable();
        this.classReference = classReference;
    }

    @Override
    public JClassType outerType() {
        return this.outerType;
    }

    @Override
    public void setOuterType(JClassType outerType) {
        this.checkUnmodifiable();
        this.outerType = outerType;
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
    public JClassNamespace namespace() {
        return classReference.namespace();
    }

    @Override
    public void setNamespace(JClassNamespace location) {
        classReference.setNamespace(location);
    }

    @Override
    public int modifiers() {
        return classReference.modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        classReference.setModifiers(modifiers);
    }

    @Override
    public JClassReference outerClass() {
        return classReference.outerClass();
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        classReference.setOuterClass(outerClass);
    }

    @Override
    public JMethodReference outerMethod() {
        return classReference.outerMethod();
    }

    @Override
    public void setOuterMethod(JMethodReference outerMethod) {
        classReference.setOuterMethod(outerMethod);
    }

    @Override
    public List<JMethodReference> declaredMethods() {
        return classReference.declaredMethods();
    }

    @Override
    public void setDeclaredMethods(List<JMethodReference> methods) {
        classReference.setDeclaredMethods(methods);
    }

    @Override
    public List<JFieldReference> declaredFields() {
        return classReference.declaredFields();
    }

    @Override
    public void setDeclaredFields(List<JFieldReference> fields) {
        classReference.setDeclaredFields(fields);
    }

    @Override
    public JClassType superClass() {
        return classReference.superClass();
    }

    @Override
    public void setSuperClass(JClassType superClass) {
        classReference.setSuperClass(superClass);
    }

    @Override
    public List<JClassType> interfaces() {
        return classReference.interfaces();
    }

    @Override
    public void setInterfaces(List<JClassType> interfaces) {
        classReference.setInterfaces(interfaces);
    }

    @Override
    public List<JVarType> typeParameters() {
        return classReference.typeParameters();
    }

    @Override
    public void setTypeParameters(List<JVarType> typeParameters) {
        classReference.setTypeParameters(typeParameters);
    }

    @Override
    public boolean hasSupertype(JClassReference supertype) {
        return classReference.hasSupertype(supertype);
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JClassType ct) {
            if (JType.equals(ct.classReference(), this.classReference, kind, seen)) {
                if (ct instanceof JClassReference cr) {
                    return !this.hasAnyTypeArguments() || JType.equals(typeArguments, cr.typeParameters(), kind, seen);
                } else if (ct instanceof JParameterizedClassType pct) {
                    return JType.equals(typeArguments, pct.typeArguments(), kind, seen) &&
                            ((!this.hasRelevantOuterType() && !pct.hasRelevantOuterType()) || JType.equals(outerType, pct.outerType(), kind, seen));
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

        if (JType.structuralEquals(this, this.classReference)) {
            return JType.hashCode(classReference, seen);
        } else {
            return JType.multiHash(JType.hashCode(classReference, seen), JType.hashCode(outerType, seen),
                    JType.hashCode(typeArguments, seen));
        }
    }

}
