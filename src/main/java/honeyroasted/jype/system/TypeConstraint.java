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

    class And implements TypeConstraint {
        private List<TypeConstraint> constraints;

        public And(List<TypeConstraint> constraints) {
            this.constraints = List.copyOf(constraints);
        }

        public List<TypeConstraint> constraints() {
            return constraints;
        }

        @Override
        public TypeConstraint and(TypeConstraint... others) {
            List<TypeConstraint> constraints = new ArrayList<>();
            constraints.addAll(this.constraints);
            Collections.addAll(constraints, others);
            return new And(constraints);
        }

        @Override
        public TypeConstraint flatten() {
            List<TypeConstraint> res = new ArrayList<>();
            this.constraints.stream().map(TypeConstraint::flatten).forEach(t -> {
                if (t instanceof And and) {
                    res.addAll(and.constraints());
                } else {
                    res.add(t);
                }
            });

            if (res.size() == 1) {
                return res.get(0);
            } else {
                return new And(res);
            }
        }

        @Override
        public void walk(Consumer<TypeConstraint> consumer) {
            TypeConstraint.super.walk(consumer);
            this.constraints.forEach(t -> t.walk(consumer));
        }

        @Override
        public String toString() {
            return "(" + this.constraints.stream().map(TypeConstraint::toString).collect(Collectors.joining(" & ")) + ")";
        }
    }

    class Or implements TypeConstraint {
        private List<TypeConstraint> constraints;

        public Or(List<TypeConstraint> constraints) {
            this.constraints = List.copyOf(constraints);
        }

        public List<TypeConstraint> constraints() {
            return constraints;
        }

        @Override
        public TypeConstraint or(TypeConstraint... others) {
            List<TypeConstraint> constraints = new ArrayList<>();
            constraints.addAll(this.constraints);
            Collections.addAll(constraints, others);
            return new Or(constraints);
        }

        @Override
        public TypeConstraint flatten() {
            List<TypeConstraint> res = new ArrayList<>();
            this.constraints.stream().map(TypeConstraint::flatten).forEach(t -> {
                if (t instanceof Or or) {
                    res.addAll(or.constraints());
                } else {
                    res.add(t);
                }
            });

            if (res.size() == 1) {
                return res.get(0);
            } else {
                return new Or(res);
            }
        }

        @Override
        public void walk(Consumer<TypeConstraint> consumer) {
            TypeConstraint.super.walk(consumer);
            this.constraints.forEach(t -> t.walk(consumer));
        }

        @Override
        public String toString() {
            return "(" + this.constraints.stream().map(TypeConstraint::toString).collect(Collectors.joining(" | ")) + ")";
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

    default TypeConstraint and(TypeConstraint... others) {
        List<TypeConstraint> constraints = new ArrayList<>();
        constraints.add(this);
        Collections.addAll(constraints, others);
        return new And(constraints);
    }

    default TypeConstraint or(TypeConstraint... others) {
        List<TypeConstraint> constraints = new ArrayList<>();
        constraints.add(this);
        Collections.addAll(constraints, others);
        return new Or(constraints);
    }

    default TypeConstraint flatten() {
        return this;
    }

    default void walk(Consumer<TypeConstraint> consumer) {
        consumer.accept(this);
    }

}
