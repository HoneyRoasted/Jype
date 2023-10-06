package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.*;

import java.util.*;

public final class ParameterizedClassTypeImpl extends AbstractPossiblyUnmodifiableType implements ParameterizedClassType {
    private ClassReference classReference;
    private List<Type> typeArguments;

    private final transient Map<ClassReference, ParameterizedClassType> superTypes = new HashMap<>();

    public ParameterizedClassTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
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

    public TypeVisitors.Mapping<TypeCache<Type, Type>> varTypeResolver() {
        return new VarTypeResolveVisitor(varType -> this.typeParameters().contains(varType),
                varType -> {
                    for (int i = 0; i < this.typeArguments.size() && i < this.typeParameters().size(); i++) {
                        if (varType.equals(this.typeParameters().get(i))) {
                            return this.typeArguments.get(i);
                        }
                    }
                    return varType;
                });
    }

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
    public boolean isInterface() {
        return this.classReference.isInterface();
    }

    @Override
    public void setInterface(boolean isInterface) {
        this.classReference.setInterface(isInterface);
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
        if (o == null || !(o instanceof ParameterizedClassType)) return false;
        ParameterizedClassType parameterizedClassType = (ParameterizedClassType) o;
        return Objects.equals(classReference, parameterizedClassType.classReference()) && Objects.equals(typeArguments, parameterizedClassType.typeArguments());
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
