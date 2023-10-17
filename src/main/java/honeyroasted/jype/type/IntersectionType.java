package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.LinkedHashSet;
import java.util.Set;

public interface IntersectionType extends Type, PossiblyUnmodifiable {

    Set<Type> children();

    void setChildren(Set<Type> children);

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
