package honeyroasted.jype.system.solver;

import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface TypeBound {
    List<?> parameters();

    abstract class Unary<T extends Type> implements TypeBound {
        protected T type;

        public Unary(T type) {
            this.type = type;
        }

        public T type() {
            return this.type;
        }

        @Override
        public List<?> parameters() {
            return List.of(this.type);
        }
    }

    abstract class Binary<L extends Type, R extends Type> implements TypeBound {
        protected L left;
        protected R right;

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
            return List.of(this.right);
        }
    }

    class Infer extends Unary<Type> {
        public Infer(Type type) {
            super(type);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Infer o && Objects.equals(this.type, o.type);
        }

        @Override
        public String toString() {
            return "infer(" + this.type + ")";
        }
    }

    class NonCyclic extends Unary<Type> {
        public NonCyclic(Type type) {
            super(type);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NonCyclic o && Objects.equals(this.type, o.type);
        }

        @Override
        public String toString() {
            return "non-cyclic(" + this.type + ")";
        }
    }

    class Equal extends Binary<Type, Type> {
        public Equal(Type left, Type right) {
            super(left, right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Equal o && Objects.equals(this.left, o.left) && Objects.equals(this.right, o.right);
        }

        @Override
        public String toString() {
            return this.left + " EQUALS " + this.right;
        }
    }

    class Subtype extends Binary<Type, Type> {
        public Subtype(Type left, Type right) {
            super(left, right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Subtype o && Objects.equals(this.left, o.left) && Objects.equals(this.right, o.right);
        }

        @Override
        public String toString() {
            return this.left + " IS A SUBTYPE OF " + this.right;
        }
    }

    class GenericParameter extends Binary<Type, Type> {
        public GenericParameter(Type left, Type right) {
            super(left, right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof GenericParameter o && Objects.equals(this.left, o.left) && Objects.equals(this.right, o.right);
        }

        @Override
        public String toString() {
            return this.left + " IS COMPATIBLE GENERIC ARGUMENT WITH " + this.right;
        }
    }

    class Unchecked extends Binary<Type, Type> {
        public Unchecked(Type left, Type right) {
            super(left, right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Unchecked o && Objects.equals(this.left, o.left) && Objects.equals(this.right, o.right);
        }

        @Override
        public String toString() {
            return this.left + " IS AN UNCHECKED SUBTYPE OF " + this.right;
        }
    }

    record Not(TypeBound child) implements TypeBound {
        @Override
        public List<?> parameters() {
            return List.of(child);
        }

        @Override
        public String toString() {
            return "not(" + this.child + ")";
        }
    }

    record Or(List<TypeBound> children) implements TypeBound {
        @Override
        public List<?> parameters() {
            return this.children;
        }

        @Override
        public String toString() {
            return "and(" + this.children.stream().map(Objects::toString).collect(Collectors.joining(", ")) + ")";
        }
    }

    record And(List<TypeBound> children) implements TypeBound {
        @Override
        public List<?> parameters() {
            return this.children;
        }

        @Override
        public String toString() {
            return "or(" + this.children.stream().map(Objects::toString).collect(Collectors.joining(", ")) + ")";
        }
    }

    final class Result {
        private TypeBound bound;
        private Propagation propagation;
        private boolean satisfied;
        private Result originator;
        private List<Result> children;

        public Result(TypeBound bound, Propagation propagation, boolean satisfied, Result originator, List<Result> children) {
            this.bound = bound;
            this.propagation = propagation;
            this.satisfied = satisfied;
            this.originator = originator;
            this.children = children;
        }

        void setOriginator(Result originator) {
            this.originator = originator;
        }

        public TypeBound bound() {
            return this.bound;
        }

        public boolean satisfied() {
            return this.satisfied;
        }

        public boolean unsatisfied() {
            return !this.satisfied;
        }

        public Result originator() {
            return this.originator;
        }

        public List<Result> children() {
            return this.children;
        }

        public static Builder builder(TypeBound bound) {
            return new Builder().setBound(bound);
        }

        public String toString(int indent) {
            String primInd = indent == 0 ? "" : "    ".repeat(indent-1) + "|----";
            String ind = indent == 0 ? "|" : "    ".repeat(indent-1) + "    |";

            StringBuilder sb = new StringBuilder();
            sb.append(primInd).append("Bound: ").append(this.bound).append("\n")
                    .append(ind).append("Satisfied: ").append(this.satisfied);

            if (!this.children.isEmpty()) {
                sb.append("\n").append(ind).append("Propagation: ").append(this.propagation).append("\n")
                        .append(ind).append("Children:\n");
                for (int i = 0; i < this.children.size(); i++) {
                    sb.append(this.children.get(i).toString(indent + 1));
                    if (i < this.children.size() - 1) {
                        sb.append("\n");
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return this.toString(0);
        }

        public static Builder builder(TypeBound bound, Propagation propagation) {
            return new Builder().setBound(bound).setPropagation(propagation);
        }

        public static Builder builder(TypeBound bound, Builder originator, Propagation propagation) {
            Builder builder = new Builder().setBound(bound).setOriginator(originator).setPropagation(propagation);
            originator.addChildren(builder);
            return builder;
        }

        public static Builder builder(TypeBound bound, Builder originator) {
            Builder builder = new Builder().setBound(bound).setOriginator(originator);
            originator.addChildren(builder);
            return builder;
        }


        public enum Propagation {
            NONE,
            AND,
            NAND,
            OR,
            NOR
        }

        public static class Builder {
            private Result built;
            private Propagation propagation = Propagation.NONE;

            private TypeBound bound;
            private boolean satisfied;
            private Builder originator;
            private List<Builder> children = new ArrayList<>();

            public Result build() {
                if (this.built == null) {
                    this.propagate();

                    List<Result> builtChildren = new ArrayList<>();
                    this.built = new Result(this.bound, this.propagation, this.satisfied, null, Collections.unmodifiableList(builtChildren));
                    if (this.originator != null) {
                        this.built.setOriginator(this.originator.build());
                    }
                    this.children.forEach(b -> builtChildren.add(b.build()));
                }
                return this.built;
            }

            public Builder propagate() {
                this.children.forEach(Builder::propagate);
                switch (this.propagation) {
                    case AND -> this.andChildren();
                    case OR -> this.orChildren();
                    case NAND -> this.andChildren().not();
                    case NOR -> this.orChildren().not();
                }
                return this;
            }

            public Builder not() {
                this.satisfied = !this.satisfied;
                return this;
            }

            public Builder andChildren() {
                this.satisfied = this.children.stream().allMatch(Builder::satisfied);
                return this;
            }

            public Builder orChildren() {
                this.satisfied = this.children.stream().anyMatch(Builder::satisfied);
                return this;
            }

            public TypeBound bound() {
                return this.bound;
            }

            public Builder setBound(TypeBound bound) {
                this.bound = bound;
                return this;
            }

            public boolean satisfied() {
                return this.satisfied;
            }

            public Builder setSatisfied(boolean satisfied) {
                this.satisfied = satisfied;
                return this;
            }

            public Builder originator() {
                return this.originator;
            }

            public Builder setOriginator(Builder originator) {
                this.originator = originator;
                return this;
            }

            public List<Builder> children() {
                return this.children;
            }

            public Builder setChildren(List<Builder> children) {
                this.children = children;
                return this;
            }

            public Builder addChildren(Builder... children) {
                Collections.addAll(this.children, children);
                return this;
            }

            public Propagation propagation() {
                return this.propagation;
            }

            public Builder setPropagation(Propagation propagation) {
                this.propagation = propagation;
                return this;
            }
        }

    }

}