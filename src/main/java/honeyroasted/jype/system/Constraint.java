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
    Constraint UNKNOWN = new Unknown();

    class Unknown implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this;
        }
    }

    class False implements Constraint {
        @Override
        public Constraint not() {
            return TRUE;
        }

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
        public Constraint not() {
            return FALSE;
        }

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

    class Not implements Constraint {
        private Constraint constraint;

        public Not(Constraint constraint) {
            this.constraint = constraint;
        }

        public Constraint constraint() {
            return constraint;
        }

        @Override
        public Constraint not() {
            return this.constraint;
        }

        @Override
        public Constraint simplify() {
            return this.constraint.simplify().not();
        }

        @Override
        public Constraint forceResolve() {
            return this.constraint.forceResolve().not();
        }

        @Override
        public String toString() {
            return "!" + this.constraint;
        }
    }

    record Equal (TypeConcrete left, TypeConcrete right) implements Constraint {
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

    record BoundedTo(TypeParameterReference parameter, TypeConcrete bound) implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this.parameter.variable().bound().assignabilityTo(this.bound).forceResolve();
        }

        @Override
        public String toString() {
            return "(" + this.parameter + " <: " + bound + ")";
        }
    }

    record BoundedFrom(TypeParameterReference parameter, TypeConcrete bound) implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return this.bound.assignabilityTo(this.parameter.variable().bound()).forceResolve();
        }

        @Override
        public String toString() {
            return "(" + this.bound + " <: " + this.parameter + ")";
        }
    }

    record InferTo (TypePlaceholder type, TypeConcrete bound) implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return Constraint.UNKNOWN;
        }

        @Override
        public String toString() {
            return "(" + this.type + " <: " + this.bound + ")";
        }
    }

    record InferFrom (TypePlaceholder type, TypeConcrete bound) implements Constraint {
        @Override
        public Constraint simplify() {
            return this;
        }

        @Override
        public Constraint forceResolve() {
            return Constraint.UNKNOWN;
        }

        @Override
        public String toString() {
            return "(" + this.bound + " <: " + this.type + ")";
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

    default Constraint not() {
        return new Not(this);
    }

    Constraint simplify();

    Constraint forceResolve();

}
