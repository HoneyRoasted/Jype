package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.metadata.location.JFieldLocation;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.lang.reflect.AccessFlag;
import java.util.Set;

public interface JFieldReference extends JType, PossiblyUnmodifiable {

    JFieldLocation location();

    void setLocation(JFieldLocation location);

    JClassReference outerClass();

    void setOuterClass(JClassReference outerClass);

    JType type();

    void setType(JType type);

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

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitFieldType(this, context);
    }

    @Override
    default Set<JType> knownDirectSupertypes() {
        return Set.of(type());
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.type().hasCyclicTypeVariables(seen);
    }
}
