package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.type.TypeParameter;

import java.util.function.Consumer;

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
            return "{" + this.left.toReadable(TypeString.Context.CONCRETE).value() + "} is equal to {" + this.right.toReadable(TypeString.Context.CONCRETE).value() + "}";
        }
    }

    record Bound(TypeConcrete subtype, TypeConcrete parent) implements TypeConstraint {

        @Override
        public String toString() {
            return "{" + this.subtype.toReadable(TypeString.Context.CONCRETE).value() + "} is assignable to {" + this.parent.toReadable(TypeString.Context.CONCRETE).value() + "}";
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

    default TypeConstraint flatten() {
        return this;
    }

    default void walk(Consumer<TypeConstraint> consumer) {
        consumer.accept(this);
    }

}
