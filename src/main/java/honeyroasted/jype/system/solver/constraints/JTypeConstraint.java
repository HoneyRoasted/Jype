package honeyroasted.jype.system.solver.constraints;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface JTypeConstraint {
    JTypeConstraint TRUE = new True();
    JTypeConstraint FALSE = new False();

    String simpleName();

    List<Object> parameters();

    default boolean isMetadata() {
        return false;
    }

    default  <T extends JTypeConstraint> T createNew(List<?> parameters) {
        try {
            return (T) getClass().getConstructors()[0].newInstance(parameters.toArray());
        } catch (ArrayIndexOutOfBoundsException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException("Could not create new instance via reflection", e);
        }
    }

    static JTypeConstraint label(String name) {
        return new Label(name);
    }

    static JTypeConstraint solve() {
        return new Solve();
    }

    static JTypeConstraint and() {
        return new And();
    }

    static JTypeConstraint or() {
        return new Or();
    }

    abstract class Unary<T> implements JTypeConstraint {
        private T value;

        public Unary(T value) {
            this.value = value;
        }

        public T value() {
            return this.value;
        }


        @Override
        public List<Object> parameters() {
            return List.of(this.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unary<?> unary = (Unary<?>) o;
            return Objects.equals(value, unary.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    abstract class Binary<L, R> implements JTypeConstraint {
        private L left;
        private R right;

        public Binary(L left, R right) {
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
            return List.of(this.left, this.right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Binary<?, ?> binary = (Binary<?, ?>) o;
            return Objects.equals(left, binary.left) && Objects.equals(right, binary.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }

    abstract class Trinary<L, M, R> implements JTypeConstraint {
        private L left;
        private M middle;
        private R right;

        public Trinary(L left, M middle, R right) {
            this.left = left;
            this.middle = middle;
            this.right = right;
        }

        public L left() {
            return this.left;
        }

        public M middle() {
            return this.middle;
        }

        public R right() {
            return this.right;
        }

        @Override
        public List<Object> parameters() {
            return List.of(this.left, this.middle, this.right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Trinary<?, ?, ?> trinary = (Trinary<?, ?, ?>) o;
            return Objects.equals(left, trinary.left) && Objects.equals(middle, trinary.middle) && Objects.equals(right, trinary.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, middle, right);
        }
    }

    abstract class UniqueNamed implements JTypeConstraint {
        private String name;

        public UniqueNamed(String name) {
            this.name = name;
        }

        @Override
        public String simpleName() {
            return this.name;
        }

        @Override
        public String toString() {
            return simpleName();
        }

        @Override
        public List<Object> parameters() {
            return Collections.emptyList();
        }

        @Override
        public boolean isMetadata() {
            return true;
        }
    }

    class Solve extends UniqueNamed {
        public Solve() {
            super("Solve child constraints");
        }
    }

    class And extends UniqueNamed {
        public And() {
            super("and");
        }
    }

    class Or extends UniqueNamed {
        public Or() {
            super("or");
        }
    }

    class True extends UniqueNamed {
        public True() {
            super("true");
        }
    }

    class False extends UniqueNamed {
        public False() {
            super("false");
        }
    }

    class Label extends Unary<String> {

        public Label(String value) {
            super(value);
        }

        @Override
        public String simpleName() {
            return this.value();
        }


        @Override
        public <T extends JTypeConstraint> T createNew(List<?> parameters) {
            return (T) new Label(String.valueOf(parameters.get(0)));
        }

        @Override
        public String toString() {
            return "LABEL('" + this.value() + "')";
        }

        @Override
        public boolean isMetadata() {
            return true;
        }
    }
}
