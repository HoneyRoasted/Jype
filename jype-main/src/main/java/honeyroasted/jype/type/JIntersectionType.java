package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.LinkedHashSet;
import java.util.Set;

public interface JIntersectionType extends JType, PossiblyUnmodifiable {

    static JType of(Set<JType> types, JTypeSystem system) {
        if (types.isEmpty()) {
            return system.constants().nullType();
        } else if (types.size() == 1) {
            return types.iterator().next();
        } else {
            JIntersectionType intersectionType = system.typeFactory().newIntersectionType();
            intersectionType.setChildren(flatten(types));
            intersectionType.setUnmodifiable(true);
            return intersectionType.simplify();
        }
    }

    Set<JType> children();

    JType simplify();

    default JType flatten() {
        return JIntersectionType.of(children(), typeSystem());
    }

    boolean isSimplified();

    void setChildren(Set<JType> children);

    default boolean typeContains(JType other) {
        return this.children().stream().anyMatch(t -> t.typeEquals(other));
    }

    static Set<JType> flatten(Set<JType> children) {
        Set<JType> results = new LinkedHashSet<>();
        for (JType child : children) {
            if (child instanceof JIntersectionType t) {
                results.addAll(flatten(t.children()));
            } else {
                results.add(child);
            }
        }
        return results;
    }

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitIntersectionType(this, context);
    }
}
