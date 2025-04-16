package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.metadata.location.JClassNamespace;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface JParameterizedClassType extends PossiblyUnmodifiable, JClassType, JParameterizedType {

    void setClassReference(JClassReference classReference);

    JClassType outerType();

    void setOuterType(JClassType outerType);

    @Override
    default JClassNamespace namespace() {
        return this.classReference().namespace();
    }

    @Override
    default void setNamespace(JClassNamespace location) {
        this.classReference().setNamespace(location);
    }

    @Override
    default int modifiers() {
        return this.classReference().modifiers();
    }

    @Override
    default void setModifiers(int modifiers) {
        this.classReference().setModifiers(modifiers);
    }

    @Override
    default JClassReference outerClass() {
        return this.classReference().outerClass();
    }

    @Override
    default void setOuterClass(JClassReference outerClass) {
        this.classReference().setOuterClass(outerClass);
    }

    @Override
    default JMethodReference outerMethod() {
        return this.classReference().outerMethod();
    }

    @Override
    default void setOuterMethod(JMethodReference outerMethod) {
        this.classReference().setOuterMethod(outerMethod);
    }

    @Override
    default List<JClassReference> nestMembers() {
        return this.classReference().nestMembers();
    }

    @Override
    default void setNestMembers(List<JClassReference> nestMembers) {
        this.classReference().setNestMembers(nestMembers);
    }

    @Override
    default List<JMethodReference> declaredMethods() {
        return this.classReference().declaredMethods();
    }

    @Override
    default void setDeclaredMethods(List<JMethodReference> methods) {
        this.classReference().setDeclaredMethods(methods);
    }

    @Override
    default List<JFieldReference> declaredFields() {
        return this.classReference().declaredFields();
    }

    @Override
    default void setDeclaredFields(List<JFieldReference> fields) {
        this.classReference().setDeclaredFields(fields);
    }

    @Override
    default JClassType superClass() {
        return this.classReference().superClass();
    }

    @Override
    default void setSuperClass(JClassType superClass) {
        this.classReference().setSuperClass(superClass);
    }

    @Override
    default List<JClassType> interfaces() {
        return this.classReference().interfaces();
    }

    @Override
    default void setInterfaces(List<JClassType> interfaces) {
        this.classReference().setInterfaces(interfaces);
    }

    @Override
    default List<JVarType> typeParameters() {
        return this.classReference().typeParameters();
    }

    @Override
    default void setTypeParameters(List<JVarType> typeParameters) {
        this.classReference().setTypeParameters(typeParameters);
    }

    @Override
    default boolean hasSupertype(JClassReference supertype) {
        return this.classReference().hasSupertype(supertype);
    }

    @Override
    default Set<JType> knownDirectSupertypes() {
        Set<JType> supertypes = new LinkedHashSet<>();
        if (this.superClass() != null) {
            supertypes.add(this.directSupertype(this.superClass()));
        }
        this.interfaces().forEach(c -> supertypes.add(this.directSupertype(c)));
        return supertypes;
    }

    @Override
    default boolean hasTypeArguments() {
        return this.typeArguments() != null && !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
