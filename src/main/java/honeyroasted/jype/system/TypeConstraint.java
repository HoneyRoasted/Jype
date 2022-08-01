package honeyroasted.jype.system;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface TypeConstraint {
    TypeConstraint FALSE = new False();
    TypeConstraint TRUE = new True();

    class False implements TypeConstraint {
        @Override
        public String toString() {
            return "FALSE";
        }
    }

    class True implements TypeConstraint {
        @Override
        public String toString() {
            return "TRUE";
        }
    }

    record Equal(TypeConcrete left, TypeConcrete right) implements TypeConstraint {
        @Override
        public String toString() {
            return "(" + this.left + " = " + this.right + ")";
        }
    }

    record Bound(TypeConcrete subtype, TypeConcrete parent) implements TypeConstraint {

        @Override
        public String toString() {
            return "(" + this.subtype + " <: " + this.parent + ")";
        }

        public Kind kind() {
            if (this.subtype instanceof TypeParameter && this.parent instanceof TypeParameter) {
                return Kind.VAR_TO_VAR;
            } else if (this.subtype instanceof TypeParameter) {
                return Kind.VAR_TO_BOUND;
            } else if (this.parent instanceof TypeParameter) {
                return Kind.BOUND_TO_VAR;
            } else {
                return Kind.BOUND_TO_BOUND;
            }
        }

        public enum Kind {
            VAR_TO_BOUND,
            BOUND_TO_VAR,
            VAR_TO_VAR,
            BOUND_TO_BOUND
        }

    }

    record Throws(TypeConcrete type) implements TypeConstraint {

    }

    default TypeConstraint flatten() {
        return this;
    }

    default void walk(Consumer<TypeConstraint> consumer) {
        consumer.accept(this);
    }

}
