package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ParameterizedClassTypeImpl extends AbstractPossiblyUnmodifiableType implements ParameterizedClassType {
    private ClassReference classReference;
    private ClassType outerType;
    private List<ArgumentType> typeArguments;

    private final transient Map<ClassType, ClassType> superTypes = new HashMap<>();

    public ParameterizedClassTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        ParameterizedClassType copy = new ParameterizedClassTypeImpl(this.typeSystem());
        cache.put(this, copy);

        copy.setClassReference(this.classReference.copy(cache));
        copy.setOuterType(this.outerType == null ? outerType : outerType.copy(cache));
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
        this.superTypes.clear();
    }

    @Override
    public Optional<ClassType> relativeSupertype(ClassType superType) {
        ClassType result = this.superTypes.get(superType);
        if (result != null) {
            return Optional.of(result);
        }

        Optional<List<ClassType>> pathOpt = this.hierarchyPathTo(superType.classReference());
        if (pathOpt.isPresent()) {
            List<ClassType> path = pathOpt.get();
            if (!path.isEmpty()) {
                Iterator<ClassType> iter = path.iterator();
                ClassType curr = iter.next();
                while (iter.hasNext()) {
                    curr = curr.directSupertype(iter.next());
                }

                if (superType instanceof ParameterizedClassType pct) {
                    curr = (ParameterizedClassType) pct.varTypeResolver().visit(curr.hasTypeArguments() ? curr : curr.classReference().parameterizedWithTypeVars());
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

    public ParameterizedClassType directSupertype(ClassType supertypeInstance) {
        if (supertypeInstance instanceof ParameterizedClassType pType) {
            VarTypeResolveVisitor varTypeResolver = this.varTypeResolver();
            ParameterizedClassType res = pType.classReference().parameterized(pType.typeArguments().stream().map(t -> (ArgumentType) varTypeResolver.visit(t)).toList());

            if (this.hasRelevantOuterType() && pType.hasRelevantOuterType()) {
                Optional<ClassType> outerRelative = this.outerType().relativeSupertype(pType.outerType());
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
        } else if (supertypeInstance instanceof ClassReference rType) {
            return rType.parameterized();
        }

        return null;
    }

    public ClassReference classReference() {
        return this.classReference;
    }

    public void setClassReference(ClassReference classReference) {
        super.checkUnmodifiable();
        this.classReference = classReference;
    }

    @Override
    public ClassType outerType() {
        return this.outerType;
    }

    @Override
    public void setOuterType(ClassType outerType) {
        this.checkUnmodifiable();
        this.outerType = outerType;
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
    public ClassNamespace namespace() {
        return classReference.namespace();
    }

    @Override
    public void setNamespace(ClassNamespace location) {
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
    public ClassReference outerClass() {
        return classReference.outerClass();
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        classReference.setOuterClass(outerClass);
    }

    @Override
    public ClassType superClass() {
        return classReference.superClass();
    }

    @Override
    public void setSuperClass(ClassType superClass) {
        classReference.setSuperClass(superClass);
    }

    @Override
    public List<ClassType> interfaces() {
        return classReference.interfaces();
    }

    @Override
    public void setInterfaces(List<ClassType> interfaces) {
        classReference.setInterfaces(interfaces);
    }

    @Override
    public List<VarType> typeParameters() {
        return classReference.typeParameters();
    }

    @Override
    public void setTypeParameters(List<VarType> typeParameters) {
        classReference.setTypeParameters(typeParameters);
    }

    @Override
    public boolean hasSupertype(ClassReference supertype) {
        return classReference.hasSupertype(supertype);
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;
        seen = Type.concat(seen, Pair.identity(this, other));

        if (other instanceof ClassType ct) {
            if (Type.equals(ct.classReference(), this.classReference, kind, seen)) {
                if (ct instanceof ClassReference cr) {
                    return !this.hasAnyTypeArguments() || Type.equals(typeArguments, cr.typeParameters(), kind, seen);
                } else if (ct instanceof ParameterizedClassType pct) {
                    return Type.equals(typeArguments, pct.typeArguments(), kind, seen) &&
                            ((!this.hasRelevantOuterType() && !pct.hasRelevantOuterType()) || Type.equals(outerType, pct.outerType(), kind, seen));
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

        if (Type.structuralEquals(this, this.classReference)) {
            return Type.hashCode(classReference, seen);
        } else {
            return Type.multiHash(Type.hashCode(classReference, seen), Type.hashCode(outerType, seen),
                    Type.hashCode(typeArguments, seen));
        }
    }

}
