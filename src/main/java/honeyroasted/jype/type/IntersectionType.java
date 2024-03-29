package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.LinkedHashSet;
import java.util.Set;

public interface IntersectionType extends Type, PossiblyUnmodifiable {

    static Type of(Set<Type> types, TypeSystem system) {
        if (types.isEmpty()) {
            return system.constants().nullType();
        } else if (types.size() == 1) {
            return types.iterator().next();
        } else {
            IntersectionType intersectionType = new IntersectionTypeImpl(system);
            intersectionType.setChildren(flatten(types));
            intersectionType.setUnmodifiable(true);
            return intersectionType;
        }
    }

    Set<Type> children();

    void setChildren(Set<Type> children);

    default boolean typeContains(Type other) {
        return this.children().stream().anyMatch(t -> t.typeEquals(other));
    }

    static Set<Type> flatten(Set<Type> children) {
        Set<Type> results = new LinkedHashSet<>();
        for (Type child : children) {
            if (child instanceof IntersectionType t) {
                results.addAll(flatten(t.children()));
            } else {
                results.add(child);
            }
        }
        return results;
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitIntersectionType(this, context);
    }
}
