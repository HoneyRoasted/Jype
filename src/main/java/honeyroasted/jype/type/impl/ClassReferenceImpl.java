package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ClassReferenceImpl extends AbstractPossiblyUnmodifiableType implements ClassReference {
    private ClassNamespace namespace;
    private boolean isInterface;
    private ClassType superClass;
    private List<ClassType> interfaces = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public ClassReferenceImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public ParameterizedClassType parameterized(List<Type> typeArguments) {
        ParameterizedClassType parameterizedClassType = new ParameterizedClassTypeImpl(this.typeSystem());
        parameterizedClassType.setClassReference(this);
        parameterizedClassType.setTypeArguments(typeArguments);
        parameterizedClassType.setUnmodifiable(true);
        return parameterizedClassType;
    }

    @Override
    public ParameterizedClassType parameterized(Type... typeArguments) {
        return parameterized(List.of(typeArguments));
    }

    @Override
    public ParameterizedClassType parameterizedWithTypeVars() {
        return parameterized((List<Type>) (List) this.typeParameters);
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

    public boolean isInterface() {
        return this.isInterface;
    }

    public void setInterface(boolean isInterface) {
        super.checkUnmodifiable();
        this.isInterface = isInterface;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClassType)) return false;
        if (o instanceof ClassReference cr) {
            return Objects.equals(namespace, cr.namespace());
        } else if (o instanceof ParameterizedClassType pt) {
            return Objects.equals(namespace, pt.classReference().namespace()) && (pt.typeArguments().isEmpty() || Objects.equals(this.typeParameters, pt.typeArguments()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace);
    }

    @Override
    public String toString() {
        return this.namespace.toString();
    }

}
