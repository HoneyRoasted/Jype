package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.Set;

public interface MetaVarType extends Type, ArgumentType {

    int identity();

    String name();

    Set<Type> upperBounds();

    Set<Type> lowerBounds();

    Set<Type> equalities();

    @Override
    default Set<Type> knownDirectSupertypes() {
        return upperBounds();
    }

    default Type upperBound() {
        if (this.upperBounds().isEmpty()) {
            return this.typeSystem().constants().object();
        } else if (this.upperBounds().size() == 1) {
            return this.upperBounds().iterator().next();
        } else {
            IntersectionType type = new IntersectionTypeImpl(this.typeSystem());
            type.setChildren(IntersectionType.flatten(upperBounds()));
            type.setUnmodifiable(true);
            return type;
        }
    }

    default Type lowerBound() {
        if (this.lowerBounds().isEmpty()) {
            return this.typeSystem().constants().nullType();
        } else if (this.lowerBounds().size() == 1) {
            return this.lowerBounds().iterator().next();
        } else {
            IntersectionType type = new IntersectionTypeImpl(this.typeSystem());
            type.setChildren(IntersectionType.flatten(lowerBounds()));
            type.setUnmodifiable(true);
            return type;
        }
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMetaVarType(this, context);
    }
}
