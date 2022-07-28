package honeyroasted.jype.system;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface TypeConstraint {
    TypeConstraint FALSE = new False();
    TypeConstraint TRUE = new True();

    class False implements TypeConstraint {
        @Override
        public TypeConstraint simplify() {
            return this;
        }

        @Override
        public TypeConstraint forceResolve() {
            return this;
        }

        @Override
        public String toString() {
            return "FALSE";
        }
    }

    class True implements TypeConstraint {
        @Override
        public TypeConstraint simplify() {
            return this;
        }

        @Override
        public TypeConstraint forceResolve() {
            return this;
        }

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
        public TypeConstraint simplify() {
            List<TypeConstraint> simple = this.constraints.stream().map(TypeConstraint::simplify).toList();
            if (simple.stream().anyMatch(c -> c instanceof False)) {
                return TypeConstraint.FALSE;
            } else if (simple.stream().allMatch(c -> c instanceof True)) {
                return TypeConstraint.TRUE;
            } else if (simple.size() == 1) {
                return simple.get(0);
            }

            return new And(simple);
        }

        @Override
        public TypeConstraint forceResolve() {
            List<TypeConstraint> resolved = this.constraints.stream().map(TypeConstraint::forceResolve).toList();
            if (resolved.stream().anyMatch(c -> c instanceof False)) {
                return TypeConstraint.FALSE;
            } else if (resolved.stream().allMatch(c -> c instanceof True)) {
                return TypeConstraint.TRUE;
            } else if (resolved.size() == 1) {
                return resolved.get(0);
            }

            return new And(resolved);
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
        public TypeConstraint simplify() {
            List<TypeConstraint> simple = this.constraints.stream().map(TypeConstraint::simplify).toList();
            if (simple.stream().anyMatch(c -> c instanceof True)) {
                return TypeConstraint.TRUE;
            } else if (simple.stream().allMatch(c -> c instanceof False)) {
                return TypeConstraint.FALSE;
            } else if (simple.size() == 1) {
                return simple.get(0);
            }

            return new Or(simple);
        }

        @Override
        public TypeConstraint forceResolve() {
            List<TypeConstraint> resolved = this.constraints.stream().map(TypeConstraint::forceResolve).toList();
            if (resolved.stream().anyMatch(c -> c instanceof True)) {
                return TypeConstraint.TRUE;
            } else if (resolved.stream().allMatch(c -> c instanceof False)) {
                return TypeConstraint.FALSE;
            } else if (resolved.size() == 1) {
                return resolved.get(0);
            }

            return new Or(resolved);
        }

        @Override
        public String toString() {
            return "(" + this.constraints.stream().map(TypeConstraint::toString).collect(Collectors.joining(" | ")) + ")";
        }
    }

    record Equal(TypeConcrete left, TypeConcrete right) implements TypeConstraint {
        @Override
        public TypeConstraint simplify() {
            return this;
        }

        @Override
        public TypeConstraint forceResolve() {
            return this.left.equals(this.right) ? TypeConstraint.TRUE : TypeConstraint.FALSE;
        }

        @Override
        public String toString() {
            return "(" + this.left + " = " + this.right + ")";
        }
    }

    record Bound(TypeConcrete subtype, TypeConcrete parent) implements TypeConstraint {

        @Override
        public TypeConstraint simplify() {
            return this;
        }

        @Override
        public TypeConstraint forceResolve() {
            if (this.subtype instanceof TypeParameter ref) {
                return ref.bound().assignabilityTo(this.parent).forceResolve();
            } else if (this.parent instanceof TypeParameter ref) {
                return this.subtype.assignabilityTo(ref.bound()).forceResolve();
            } else {
                return this.subtype.assignabilityTo(this.parent).forceResolve();
            }
        }

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

    TypeConstraint simplify();

    TypeConstraint forceResolve();

}
