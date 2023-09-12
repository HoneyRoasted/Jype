package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ClassReference extends AbstractPossiblyUnmodifiableType {
    private ClassNamespace namespace;
    private ClassReference superClass;
    private List<ClassReference> interfaces = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public ClassReference(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.interfaces = List.copyOf(this.interfaces);
        this.typeParameters = List.copyOf(this.typeParameters);
    }

    public ClassNamespace namespace() {
        return namespace;
    }

    public void setNamespace(ClassNamespace location) {
        super.checkUnmodifiable();
        this.namespace = location;
    }

    public ClassReference superClass() {
        return superClass;
    }

    public void setSuperClass(ClassReference superClass) {
        super.checkUnmodifiable();
        this.superClass = superClass;
    }

    public List<ClassReference> interfaces() {
        return interfaces;
    }

    public void setInterfaces(List<ClassReference> interfaces) {
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

            for (ClassReference inter : this.interfaces) {
                if (inter.hasSupertype(supertype)) {
                    return true;
                }
            }

            return false;
        }
    }

    public Optional<List<ClassReference>> pathTo(ClassReference supertype) {
        List<ClassReference> building = new ArrayList<>();
        if (pathImpl(supertype, building)) {
            return Optional.of(building);
        }
        return Optional.empty();
    }

    private boolean pathImpl(ClassReference supertype, List<ClassReference> building) {
        if (supertype == null) return false;

        if (this.equals(supertype)) {
            building.add(0, supertype);
            return true;
        } else {
            if (this.pathImpl(this.superClass, building)) {
                building.add(0, supertype);
                return true;
            }

            for (ClassReference inter : this.interfaces) {
                if (this.pathImpl(inter, building)) {
                    building.add(0, supertype);
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassReference that = (ClassReference) o;
        return Objects.equals(namespace, that.namespace);
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
