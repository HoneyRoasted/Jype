package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ParameterizedClassTypeImpl extends AbstractPossiblyUnmodifiableType implements ParameterizedClassType {
    private ClassReference classReference;
    private ClassType outerType;
    private List<Type> typeArguments;

    private final transient Map<ClassReference, ParameterizedClassType> superTypes = new HashMap<>();

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
        copy.setTypeArguments(this.typeArguments.stream().map(t -> (Type) t.copy(cache)).toList());
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
    public Optional<ClassType> relativeSupertype(ClassReference superType) {
        ClassType result = this.superTypes.get(superType);
        if (result != null) {
            return Optional.of(result);
        }

        Optional<List<ClassType>> pathOpt = this.hierarchyPathTo(superType);
        if (pathOpt.isPresent()) {
            List<ClassType> path = pathOpt.get();
            if (!path.isEmpty()) {
                Iterator<ClassType> iter = path.iterator();
                ClassType nxt = iter.next();

                ParameterizedClassType curr = nxt instanceof ParameterizedClassType ptype ? ptype :
                        nxt.classReference().parameterizedWithTypeVars();
                while (iter.hasNext()) {
                    curr = curr.directSupertype(iter.next());
                }

                this.superTypes.put(superType, curr);
                return Optional.of(curr);
            }
        }

        return Optional.empty();
    }

    public ParameterizedClassType directSupertype(ClassType supertypeInstance) {
        if (supertypeInstance instanceof ParameterizedClassType pType) {
            return pType.classReference()
                    .parameterized(pType.typeArguments().stream().map(this.varTypeResolver()).toList());
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
    public List<Type> typeArguments() {
        return this.typeArguments;
    }

    @Override
    public void setTypeArguments(List<Type> typeArguments) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClassType)) return false;
        if (o instanceof ParameterizedClassType pct) {
            return Objects.equals(classReference, pct.classReference()) && Objects.equals(typeArguments, pct.typeArguments()) && (Modifier.isStatic(this.modifiers()) || Objects.equals(outerType, pct.outerType()));
        } else if (o instanceof ClassReference cr) {
            return Objects.equals(classReference, cr) && (typeArguments.isEmpty() || Objects.equals(typeArguments, cr.typeParameters())) && (Modifier.isStatic(this.modifiers()) || Objects.equals(outerType, cr.outerClass()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classReference, typeArguments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.classReference);
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
