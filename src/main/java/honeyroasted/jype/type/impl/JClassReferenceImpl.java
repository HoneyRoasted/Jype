package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JInMemoryTypeCache;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class JClassReferenceImpl extends JAbstractPossiblyUnmodifiableType implements JClassReference {
    private JClassNamespace namespace;
    private int modifiers;
    private JClassReference outerClass;
    private JMethodReference outerMethod;
    private JClassType superClass;
    private List<JClassType> interfaces = new ArrayList<>();
    private List<JVarType> typeParameters = new ArrayList<>();

    private List<JMethodReference> declaredMethods = new ArrayList<>();

    public JClassReferenceImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JClassReference copy = this.typeSystem().typeFactory().newClassReference();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setNamespace(this.namespace);
        copy.setModifiers(this.modifiers);
        copy.setOuterClass(this.outerClass == null ? this.outerClass : this.outerClass.copy(cache));
        copy.setOuterMethod(this.outerMethod == null ? this.outerMethod : this.outerMethod.copy(cache));
        copy.setSuperClass(this.superClass == null ? this.superClass : this.superClass.copy(cache));
        copy.setInterfaces(this.interfaces.stream().map(c -> (JClassType) c.copy(cache)).toList());
        copy.setTypeParameters(this.typeParameters.stream().map(v -> (JVarType) v.copy(cache)).toList());
        copy.setDeclaredMethods(this.declaredMethods.stream().map(m -> (JMethodReference) m.copy(cache)).toList());
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public JParameterizedClassType parameterized(List<JArgumentType> typeArguments) {
        if (typeArguments.size() != this.typeParameters.size()) {
            throw new IllegalArgumentException("Expected exactly " + this.typeParameters.size() + " type arguments");
        }

        JParameterizedClassType parameterizedClassType = this.typeSystem().typeFactory().newParameterizedClassType();
        parameterizedClassType.setClassReference(this);
        parameterizedClassType.setTypeArguments(typeArguments);
        parameterizedClassType.setUnmodifiable(true);
        return parameterizedClassType;
    }

    @Override
    public JParameterizedClassType parameterized(JArgumentType... typeArguments) {
        return parameterized(List.of(typeArguments));
    }

    @Override
    public JParameterizedClassType parameterizedWithTypeVars() {
        return parameterized((List) this.typeParameters);
    }

    @Override
    public JParameterizedClassType parameterizedWithMetaVars() {
        List<JMetaVarType> mvts = new ArrayList<>();
        Map<JVarType, JMetaVarType> typeMap = new HashMap<>();

        this.typeParameters.forEach(v -> {
            JMetaVarType mvt = v.createMetaVar();
            mvts.add(mvt);
            typeMap.put(v, mvt);
        });

        JTypeVisitor<JType, Void> resolver = new JVarTypeResolveVisitor(typeMap)
                .withContext(new JInMemoryTypeCache<>(JType.class, JType.class));
        for (int i = 0; i < mvts.size(); i++) {
            JVarType param = this.typeParameters.get(i);
            JMetaVarType mvt = mvts.get(i);
            if (!param.hasDefaultBounds()) {
                param.upperBounds().forEach(bnd -> mvt.upperBounds().add(bnd.accept(resolver)));
            }
        }

        return parameterized((List) mvts);
    }

    @Override
    protected void makeUnmodifiable() {
        this.interfaces = List.copyOf(this.interfaces);
        this.typeParameters = List.copyOf(this.typeParameters);
        this.declaredMethods = List.copyOf(this.declaredMethods);
    }

    @Override
    protected void makeModifiable() {
        this.interfaces = new ArrayList<>(this.interfaces);
        this.typeParameters = new ArrayList<>(this.typeParameters);
        this.declaredMethods = new ArrayList<>(this.declaredMethods);
    }

    public JClassNamespace namespace() {
        return namespace;
    }

    public void setNamespace(JClassNamespace location) {
        super.checkUnmodifiable();
        this.namespace = location;
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

    @Override
    public JMethodReference outerMethod() {
        return this.outerMethod;
    }

    @Override
    public void setOuterMethod(JMethodReference outerMethod) {
        this.checkUnmodifiable();
        this.outerMethod = outerMethod;
    }

    @Override
    public List<JMethodReference> declaredMethods() {
        return this.declaredMethods;
    }

    @Override
    public void setDeclaredMethods(List<JMethodReference> methods) {
        checkUnmodifiable();
        this.declaredMethods = methods;
    }

    @Override
    public JParameterizedClassType directSupertype(JClassType supertypeInstance) {
        return this.parameterizedWithTypeVars().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<JClassType> relativeSupertype(JClassType superType) {
        return this.parameterizedWithTypeVars().relativeSupertype(superType);
    }

    public JClassType superClass() {
        return superClass;
    }

    public void setSuperClass(JClassType superClass) {
        super.checkUnmodifiable();
        this.superClass = superClass;
    }

    public List<JClassType> interfaces() {
        return interfaces;
    }

    public void setInterfaces(List<JClassType> interfaces) {
        super.checkUnmodifiable();
        this.interfaces = interfaces;
    }

    public List<JVarType> typeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<JVarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    public boolean hasSupertype(JClassReference supertype) {
        if (this.equals(supertype)) {
            return true;
        } else {
            if (this.superClass != null && this.superClass.hasSupertype(supertype)) {
                return true;
            }

            for (JClassType inter : this.interfaces) {
                if (inter.hasSupertype(supertype)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public JClassReference classReference() {
        return this;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JClassType ct) {
            if (Objects.equals(namespace, ct.namespace()) && modifiers == ct.modifiers() &&
                    ((!this.hasRelevantOuterType() && !ct.hasRelevantOuterType()) || JType.equals(this.outerClass, ct.outerType(), kind, seen))) {
                if (other instanceof JParameterizedClassType pct) {
                    return (!pct.hasTypeArguments() || JType.equals(pct.typeArguments(), typeParameters, kind, seen));
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

        return JType.multiHash(Objects.hashCode(this.namespace), modifiers, JType.hashCode(outerClass, seen));
    }

}
