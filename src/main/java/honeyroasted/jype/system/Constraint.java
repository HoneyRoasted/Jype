package honeyroasted.jype.system;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.concrete.TypeParameterReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface Constraint {
    Constraint FALSE = new False();
    Constraint TRUE = new True();

    class False implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this;
        }

        @Override
        public String toString() {
            return "FALSE";
        }
    }

    class True implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this;
        }

        @Override
        public String toString() {
            return "TRUE";
        }
    }

    class And implements Constraint {
        private List<Constraint> constraints;

        public And(List<Constraint> constraints) {
            this.constraints = List.copyOf(constraints);
        }

        public List<Constraint> constraints() {
            return constraints;
        }

        @Override
        public Constraint and(Constraint... others) {
            List<Constraint> constraints = new ArrayList<>();
            constraints.addAll(this.constraints);
            Collections.addAll(constraints, others);
            return new And(constraints);
        }

        @Override
        public Constraint simplify() {
            List<Constraint> simple = this.constraints.stream().map(Constraint::simplify).toList();
            if (simple.stream().anyMatch(c -> c instanceof False)) {
                return Constraint.FALSE;
            } else if (simple.stream().allMatch(c -> c instanceof True)) {
                return Constraint.TRUE;
            } else if (simple.size() == 1) {
                return simple.get(0);
            }

            return new And(simple);
        }

        @Override
        public Constraint forceResolve() {
            List<Constraint> resolved = this.constraints.stream().map(Constraint::forceResolve).toList();
            if (resolved.stream().anyMatch(c -> c instanceof False)) {
                return Constraint.FALSE;
            } else if (resolved.stream().allMatch(c -> c instanceof True)) {
                return Constraint.TRUE;
            } else if (resolved.size() == 1) {
                return resolved.get(0);
            }

            return new And(resolved);
        }

        @Override
        public String toString() {
            return "(" + this.constraints.stream().map(Constraint::toString).collect(Collectors.joining(" & ")) + ")";
        }
    }

    class Or implements Constraint {
        private List<Constraint> constraints;

        public Or(List<Constraint> constraints) {
            this.constraints = List.copyOf(constraints);
        }

        public List<Constraint> constraints() {
            return constraints;
        }

        @Override
        public Constraint or(Constraint... others) {
            List<Constraint> constraints = new ArrayList<>();
            constraints.addAll(this.constraints);
            Collections.addAll(constraints, others);
            return new Or(constraints);
        }

        @Override
        public Constraint simplify() {
            List<Constraint> simple = this.constraints.stream().map(Constraint::simplify).toList();
            if (simple.stream().anyMatch(c -> c instanceof True)) {
                return Constraint.TRUE;
            } else if (simple.stream().allMatch(c -> c instanceof False)) {
                return Constraint.FALSE;
            } else if (simple.size() == 1) {
                return simple.get(0);
            }

            return new Or(simple);
        }

        @Override
        public Constraint forceResolve() {
            List<Constraint> resolved = this.constraints.stream().map(Constraint::forceResolve).toList();
            if (resolved.stream().anyMatch(c -> c instanceof True)) {
                return Constraint.TRUE;
            } else if (resolved.stream().allMatch(c -> c instanceof False)) {
                return Constraint.FALSE;
            } else if (resolved.size() == 1) {
                return resolved.get(0);
            }

            return new Or(resolved);
        }

        @Override
        public String toString() {
            return "(" + this.constraints.stream().map(Constraint::toString).collect(Collectors.joining(" | ")) + ")";
        }
    }

    record Equal(TypeConcrete left, TypeConcrete right) implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this.left.equals(this.right) ? Constraint.TRUE : Constraint.FALSE;
        }

        @Override
        public String toString() {
            return "(" + this.left + " = " + this.right + ")";
        }
    }

    record Bound(TypeConcrete subtype, TypeConcrete parent) implements Constraint {

        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            if (this.subtype instanceof TypeParameterReference ref) {
                return ref.variable().bound().assignabilityTo(this.parent).forceResolve();
            } else if (this.parent instanceof TypeParameterReference ref) {
                return this.subtype.assignabilityTo(ref.variable().bound()).forceResolve();
            } else if (this.subtype instanceof TypePlaceholder || this.parent instanceof TypePlaceholder) {
                return this;
            } else {
                return this.subtype.assignabilityTo(this.parent).forceResolve();
            }
        }

        @Override
        public String toString() {
            return "(" + this.subtype + " <: " + this.parent + ")";
        }
    }

    default Constraint and(Constraint... others) {
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(this);
        Collections.addAll(constraints, others);
        return new And(constraints);
    }

    default Constraint or(Constraint... others) {
        List<Constraint> constraints = new ArrayList<>();
        constraints.add(this);
        Collections.addAll(constraints, others);
        return new Or(constraints);
    }

    Constraint simplify();

    Constraint forceResolve();

}
