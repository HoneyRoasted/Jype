package honeyroasted.jype.system.solver;

import honeyroasted.jype.type.Type;

import java.util.List;

public interface TypeBound {
    List<?> parameters();

    abstract class TypeComparison<L extends Type, R extends Type> implements TypeBound {
        private L left;
        private R right;

        public TypeComparison(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L left() {
            return this.left;
        }

        public R right() {
            return this.right;
        }

        @Override
        public List<Object> parameters() {
            return List.of(this.right);
        }
    }

    class Equal extends TypeComparison<Type, Type> {
        public Equal(Type left, Type right) {
            super(left, right);
        }
    }

    class Subtype extends TypeComparison<Type, Type> {
        public Subtype(Type left, Type right) {
            super(left, right);
        }
    }

    record Or(List<TypeBound> children) implements TypeBound {
        @Override
        public List<?> parameters() {
            return this.children;
        }
    }

    record And(List<TypeBound> children) implements TypeBound {
        @Override
        public List<?> parameters() {
            return this.children;
        }
    }

    record Result(TypeBound bound, boolean satisfied, Result originator, List<Result> children) {}

}
