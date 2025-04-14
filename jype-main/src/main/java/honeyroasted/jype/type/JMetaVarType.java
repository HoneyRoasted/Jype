package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.Set;

public interface JMetaVarType extends JType, JArgumentType {

    int identity();

    String name();

    Set<JType> upperBounds();

    Set<JType> lowerBounds();

    Set<JType> equalities();

    @Override
    default Set<JType> knownDirectSupertypes() {
        return upperBounds();
    }

    default JType upperBound() {
        if (this.upperBounds().isEmpty()) {
            return this.typeSystem().constants().object();
        } else if (this.upperBounds().size() == 1) {
            return this.upperBounds().iterator().next();
        } else {
            return JIntersectionType.of(upperBounds(), this.typeSystem());
        }
    }

    default JType lowerBound() {
        if (this.lowerBounds().isEmpty()) {
            return this.typeSystem().constants().nullType();
        } else if (this.lowerBounds().size() == 1) {
            return this.lowerBounds().iterator().next();
        } else {
            return JIntersectionType.of(lowerBounds(), this.typeSystem());
        }
    }

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitMetaVarType(this, context);
    }
}
