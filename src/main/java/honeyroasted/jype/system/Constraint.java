package honeyroasted.jype.system;

import honeyroasted.jype.concrete.TypeClass;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypeInferable;
import honeyroasted.jype.concrete.TypeParameterReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface Constraint {
    Constraint FALSE = new False();
    Constraint TRUE = new True();

    class False implements Constraint {
        @Override
        public Constraint not() {
            return TRUE;
        }
    }

    class True implements Constraint {
        @Override
        public Constraint not() {
            return FALSE;
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
    }

    record Equal (TypeConcrete left, TypeConcrete right) implements Constraint {}

    record Bounded (TypeParameterReference parameter, TypeConcrete bound) implements Constraint {}

    record InferInto (TypeInferable type, TypeConcrete bound) implements Constraint {}

    record InferFrom (TypeInferable type, TypeConcrete bound) implements Constraint {}

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

}
