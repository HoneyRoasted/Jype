package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ClassReferenceImpl extends AbstractPossiblyUnmodifiableType implements ClassReference {
    private ClassNamespace namespace;
    private int modifiers;
    private ClassReference outerClass;
    private ClassType superClass;
    private List<ClassType> interfaces = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public ClassReferenceImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        ClassReference copy = new ClassReferenceImpl(this.typeSystem());
        cache.put(this, copy);

        copy.setNamespace(this.namespace);
        copy.setModifiers(this.modifiers);
        copy.setOuterClass(this.outerClass == null ? this.outerClass : this.outerClass.copy(cache));
        copy.setSuperClass(this.superClass == null ? this.superClass : this.superClass.copy(cache));
        copy.setInterfaces(this.interfaces.stream().map(c -> (ClassType) c.copy(cache)).toList());
        copy.setTypeParameters(this.typeParameters.stream().map(v -> (VarType) v.copy(cache)).toList());
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public ParameterizedClassType parameterized(List<ArgumentType> typeArguments) {
        ParameterizedClassType parameterizedClassType = new ParameterizedClassTypeImpl(this.typeSystem());
        parameterizedClassType.setClassReference(this);
        parameterizedClassType.setTypeArguments(typeArguments);
        parameterizedClassType.setUnmodifiable(true);
        return parameterizedClassType;
    }

    @Override
    public ParameterizedClassType parameterized(ArgumentType... typeArguments) {
        return parameterized(List.of(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterizedWithTypeVars() {
        return parameterized((List) this.typeParameters);
    }

    @Override
    public ParameterizedClassType parameterizedWithMetaVars() {
        List<MetaVarType> mvts = new ArrayList<>();
        Map<VarType, MetaVarType> typeMap = new HashMap<>();

        this.typeParameters.forEach(v -> {
            MetaVarType mvt = new MetaVarTypeImpl(v.typeSystem(), v.name());
            mvts.add(mvt);
            typeMap.put(v, mvt);
        });

        TypeVisitor<Type, Void> resolver = new VarTypeResolveVisitor(typeMap)
                .withContext(new InMemoryTypeCache<>());
        for (int i = 0; i < mvts.size(); i++) {
            VarType param = this.typeParameters.get(i);
            MetaVarType mvt = mvts.get(i);
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
    }

    @Override
    protected void makeModifiable() {
        this.interfaces = new ArrayList<>(this.interfaces);
        this.typeParameters = new ArrayList<>(this.typeParameters);
    }

    @Override
    protected void checkUnmodifiable() {
        this.interfaces = new ArrayList<>(this.interfaces);
        this.typeParameters = new ArrayList<>(this.typeParameters);
    }

    public ClassNamespace namespace() {
        return namespace;
    }

    public void setNamespace(ClassNamespace location) {
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
    public ClassReference outerClass() {
        return this.outerClass;
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        this.checkUnmodifiable();
        this.outerClass = outerClass;
    }

    @Override
    public ParameterizedClassType directSupertype(ClassType supertypeInstance) {
        return this.parameterizedWithTypeVars().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<ClassType> relativeSupertype(ClassType superType) {
        return this.parameterizedWithTypeVars().relativeSupertype(superType);
    }

    public ClassType superClass() {
        return superClass;
    }

    public void setSuperClass(ClassType superClass) {
        super.checkUnmodifiable();
        this.superClass = superClass;
    }

    public List<ClassType> interfaces() {
        return interfaces;
    }

    public void setInterfaces(List<ClassType> interfaces) {
        super.checkUnmodifiable();
        this.interfaces = interfaces;
    }

    public List<VarType> typeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(List<VarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    public boolean hasSupertype(ClassReference supertype) {
        if (this.equals(supertype)) {
            return true;
        } else {
            if (this.superClass != null && this.superClass.hasSupertype(supertype)) {
                return true;
            }

            for (ClassType inter : this.interfaces) {
                if (inter.hasSupertype(supertype)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public ClassReference classReference() {
        return this;
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;
        seen = Type.concat(seen, Pair.identity(this, other));

        if (other instanceof ClassType ct) {
            if (Objects.equals(namespace, ct.namespace()) && modifiers == ct.modifiers() &&
                    ((!this.hasRelevantOuterType() && !ct.hasRelevantOuterType()) || Type.equals(this.outerClass, ct.outerType(), kind, seen))) {
                if (other instanceof ParameterizedClassType pct) {
                    return (!pct.hasTypeArguments() || Type.equals(pct.typeArguments(), typeParameters, kind, seen));
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
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.multiHash(Objects.hashCode(this.namespace), modifiers, Type.hashCode(outerClass, seen));
    }

}
