package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.location.JGenericDeclarationLocation;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JClassType extends JGenericDeclaration, JInstantiableType, JReferencableType, PossiblyUnmodifiable, JArgumentType {

    JClassNamespace namespace();

    void setNamespace(JClassNamespace namespace);

    @Override
    default Optional<JClassNamespace> classNamespace() {
        return Optional.of(this.namespace());
    }

    @Override
    default JGenericDeclarationLocation genericDeclarationLocation() {
        return namespace();
    }

    int modifiers();

    default JAccess access() {
        return JAccess.fromFlags(modifiers());
    }

    default boolean hasModifier(JAccess flag) {
        return flag.canAccess(this.modifiers());
    }

    default boolean hasModifier(AccessFlag flag) {
        return (flag.mask() & modifiers()) != 0;
    }

    void setModifiers(int modifiers);

    JClassReference outerClass();

    void setOuterClass(JClassReference outerClass);

    JMethodReference outerMethod();

    void setOuterMethod(JMethodReference outerMethod);

    List<JClassReference> nestMembers();

    void setNestMembers(List<JClassReference> nestMembers);

    List<JMethodReference> declaredMethods();

    void setDeclaredMethods(List<JMethodReference> methods);

    List<JFieldReference> declaredFields();

    void setDeclaredFields(List<JFieldReference> fields);

    default boolean hasRelevantOuterType() {
        return !Modifier.isStatic(this.modifiers()) && this.outerType() != null
                && (this.outerMethod() == null || !Modifier.isStatic(this.outerMethod().modifiers()));
    }

    @Override
    default Optional<JVarType> resolveVarType(String name) {
        Optional<JVarType> check = this.typeParameters().stream().filter(j -> j.name().equals(name)).findFirst();
        if (check.isPresent()) {
            return check;
        } else if (!Modifier.isStatic(this.modifiers()) && this.outerMethod() != null) {
            return this.outerMethod().resolveVarType(name);
        } else if (!Modifier.isStatic(this.modifiers()) && this.outerType() != null) {
            return this.outerType().resolveVarType(name);
        } else {
            return Optional.empty();
        }
    }

    default boolean hasOuterType() {
        return this.outerType() != null;
    }

    JParameterizedClassType directSupertype(JClassType supertypeInstance);

    Optional<JClassType> relativeSupertype(JClassType superType);

    default JAccess accessFrom(JClassType other) {
        return this.accessFrom(other, null);
    }

    default JAccess accessFrom(JClassType other, JClassType receiverType) {
        if (outermost(this.classReference()).equals(outermost(other.classReference()))) {
            // JLS §6.6.1 private access within the enclosing top level class
            return JAccess.PRIVATE;
        } else if (this.namespace().location().getPackage().equals(other.namespace().location().getPackage())) {
            // JLS §6.6.1 package access
            return JAccess.PACKAGE_PROTECTED;
        }
        JClassType current = other;
        while (current != null) {
            if (current.hasSupertype(this.classReference()) && (receiverType == null || receiverType.hasSupertype(current.classReference()))) {
                // JLS §6.6.2.1 protected access within the body of a subclass
                return JAccess.PROTECTED;
            }
            current = current.hasOuterType() ? current.outerType() : null;
        }
        return JAccess.PUBLIC;
    }

    private static JClassReference outermost(JClassReference ref) {
        JClassReference current = ref;
        while (current.outerClass() != null) {
            current = current.outerClass();
        }
        return current;
    }

    @Override
    default Set<JType> knownDirectSupertypes() {
        Set<JType> res = new LinkedHashSet<>();
        if (this.superClass() != null) {
            res.add(this.superClass());
        }
        res.addAll(this.interfaces());
        return res;
    }

    JClassType outerType();

    JClassType superClass();

    void setSuperClass(JClassType superClass);

    List<JClassType> interfaces();

    void setInterfaces(List<JClassType> interfaces);

    default boolean hasTypeParameters() {
        return !this.typeParameters().isEmpty();
    }

    List<JVarType> typeParameters();

    void setTypeParameters(List<JVarType> typeParameters);

    boolean hasSupertype(JClassReference supertype);

    JClassReference classReference();

    boolean hasTypeArguments();

    default boolean hasAnyTypeArguments() {
        return this.hasTypeArguments() || (this.hasRelevantOuterType() && this.outerType().hasAnyTypeArguments());
    }

    List<JArgumentType> typeArguments();

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitClassType(this, context);
    }

    default boolean buildHierarchyPath(JClassReference supertype, List<JClassType> building) {
        if (supertype == null) return false;

        if (this.classReference().equals(supertype)) {
            building.add(0, this);
            return true;
        } else {
            if (this.superClass() != null && this.superClass().buildHierarchyPath(supertype, building)) {
                building.add(0, this);
                return true;
            }

            for (JClassType inter : this.interfaces()) {
                if (inter.buildHierarchyPath(supertype, building)) {
                    building.add(0, this);
                    return true;
                }
            }

            return false;
        }
    }

    default Optional<List<JClassType>> hierarchyPathTo(JClassReference supertype) {
        List<JClassType> building = new ArrayList<>();
        if (buildHierarchyPath(supertype, building)) {
            return Optional.of(building);
        }
        return Optional.empty();
    }


}
