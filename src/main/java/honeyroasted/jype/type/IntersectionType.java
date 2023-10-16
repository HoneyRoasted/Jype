package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Set;

public interface IntersectionType extends Type, PossiblyUnmodifiable {

    Set<Type> children();

    void setChildren(Set<Type> children);

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitIntersectionType(this, context);
    }
}
