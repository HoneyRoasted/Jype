package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.solver.solvers.inference.ExpressionInformation;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface TypeBound {
    List<?> parameters();

    String simpleName();

    interface Compound extends TypeBound {
        List<TypeBound> children();

        @Override
        default List<?> parameters() {
            return this.children();
        }
    }

    abstract class Unary<T> implements TypeBound {
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

        @Override
        public int hashCode() {
            return Objects.hashCode(this.type);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) return false;
            Unary<?> other = (Unary<?>) obj;
            return Objects.equals(this.type, other.type);
        }
    }

    abstract class Binary<L, R> implements TypeBound {
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
        public int hashCode() {
            return Objects.hash(this.left, this.right);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) return false;
            Binary<?, ?> other = (Binary<?, ?>) obj;
            return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
        }

        @Override
        public List<Object> parameters() {
            return List.of(this.left, this.right);
        }
    }

    final class NonCyclic extends Unary<Type> {
        public NonCyclic(Type type) {
            super(type);
        }

        @Override
        public String toString() {
            return this.type + " DOES NOT HAVE CYCLIC TYPE VARIABLES";
        }

        @Override
        public String simpleName() {
            return "non-cyclic(" + this.type.simpleName() + ")";
        }
    }

    final class NeedsInference extends Unary<VarType> {

        public NeedsInference(VarType type) {
            super(type);
        }

        @Override
        public String toString() {
            return this.type + " NEEDS INFERENCE";
        }

        @Override
        public String simpleName() {
            return "infer(" + this.type.simpleName() + ")";
        }
    }

    final class Equal extends Binary<Type, Type> {
        public Equal(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " EQUALS " + this.right;
        }

        @Override
        public int hashCode() {
            return this.left.hashCode() + this.right.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Equal eq &&
                    ((Objects.equals(this.left, eq.left) && Objects.equals(this.right, eq.right)) ||
                            (Objects.equals(this.left, eq.right) && Objects.equals(this.right, eq.left)));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " = " + this.right.simpleName();
        }
    }

    final class Compatible extends Binary<Type, Type> {
        public Compatible(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS COMPATIBLE WITH " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " -> " + this.right.simpleName();
        }
    }

    final class ExpressionCompatible extends Binary<ExpressionInformation, Type> {
        public ExpressionCompatible(ExpressionInformation left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS COMPATIBLE WITH " + this.right;
        }

        @Override
        public String simpleName() {
            return "*expr -> " + this.right.simpleName();
        }
    }

    final class Contains extends Binary<VarType, VarType> {
        public Contains(VarType left, VarType right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS CONTAINED BY " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " contains " + this.right.simpleName();
        }
    }

    final class LambdaThrows extends Binary<ExpressionInformation, Type> {

        public LambdaThrows(ExpressionInformation left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " THROWS " + this.right;
        }

        @Override
        public String simpleName() {
            return "*expr throws(" + this.right.simpleName() + ")";
        }
    }

    final class Throws extends Unary<VarType> {
        public Throws(VarType type) {
            super(type);
        }

        @Override
        public String toString() {
            return "THROWS " + this.type;
        }

        @Override
        public String simpleName() {
            return "throws(" + this.type.simpleName() + ")";
        }
    }

    class Captures extends Binary<Type, Type> {
        public Captures(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " CAPTURES " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " captures " + this.right.simpleName();
        }
    }

    class Subtype extends Binary<Type, Type> {
        public Subtype(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS A SUBTYPE OF " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " <: " + this.right.simpleName();
        }
    }

    class GenericParameter extends Binary<Type, Type> {
        public GenericParameter(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS A COMPATIBLE GENERIC ARGUMENT WITH " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " compatible " + this.right.simpleName();
        }
    }

    class Unchecked extends Binary<Type, Type> {
        public Unchecked(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS AN UNCHECKED SUBTYPE OF " + this.right;
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " <: " + this.right.simpleName();
        }
    }

    interface ResultView {
        boolean satisfied();

        TypeBound bound();

        Result.Propagation propagation();

        List<? extends ResultView> parents();

        List<? extends ResultView> children();
    }

    final class Result implements ResultView {
        private TypeBound bound;
        private Propagation propagation;
        private boolean satisfied;
        private List<Result> parents;
        private List<Result> children;

        public Result(TypeBound bound, Propagation propagation, boolean satisfied, List<Result> parents, List<Result> children) {
            this.bound = bound;
            this.propagation = propagation;
            this.satisfied = satisfied;
            this.parents = parents;
            this.children = children;
        }

        void setParents(List<Result> parents) {
            this.parents = parents;
        }

        public TypeBound bound() {
            return this.bound;
        }

        public Propagation propagation() {
            return this.propagation;
        }

        public boolean satisfied() {
            return this.satisfied;
        }

        public boolean unsatisfied() {
            return !this.satisfied;
        }

        public List<Result> parents() {
            return this.parents;
        }

        public List<Result> children() {
            return this.children;
        }

        public static Builder builder(TypeBound bound) {
            return new Builder().setBound(bound);
        }

        private void toString(List<String> building) {
            String ind = "    ";
            building.add("Bound: " + this.bound);
            building.add("Satisfied: " + this.satisfied);

            if (!this.children.isEmpty()) {
                building.add("Propagation: " + this.propagation);
                building.add("Children: " + this.children.size());

                List<String> children = new ArrayList<>();
                this.children.forEach(r -> r.toString(children));

                int maxLen = children.stream().mapToInt(String::length).max().getAsInt();
                String content = "-".repeat(maxLen + 8);
                String top = "+" + content + "+";
                building.add(top);
                for (String c : children) {
                    building.add("|" + ind + c + (" ".repeat(maxLen - c.length() + 4)) + "|");
                }
                building.add(top);
            }
        }

        @Override
        public String toString() {
            List<String> building = new ArrayList<>();
            this.toString(building);
            return String.join("\n", building);
        }

        public static Builder builder(TypeBound bound, Propagation propagation) {
            return new Builder().setBound(bound).setPropagation(propagation);
        }

        public static Builder builder(TypeBound bound, Builder originator, Propagation propagation) {
            Builder builder = new Builder().setBound(bound).addParents(originator).setPropagation(propagation);
            originator.addChildren(builder);
            return builder;
        }

        public static Builder builder(TypeBound bound, Builder originator) {
            Builder builder = new Builder().setBound(bound).addParents(originator);
            originator.addChildren(builder);
            return builder;
        }

        public static Builder builder(TypeBound bound, Builder... originators) {
            Builder builder = new Builder().setBound(bound).addParents(originators);
            for (Builder originator : originators) {
                originator.addChildren(builder);
            }
            return builder;
        }

        public static Builder builder(TypeBound bound, Propagation propagation, Builder... originators) {
            Builder builder = new Builder().setBound(bound).addParents(originators).setPropagation(propagation);
            for (Builder originator : originators) {
                originator.addChildren(builder);
            }
            return builder;
        }


        public enum Propagation {
            NONE,
            AND,
            NAND,
            OR,
            NOR
        }

        public static class Builder implements ResultView {
            private Result built;
            private Propagation propagation = Propagation.NONE;

            private TypeBound bound;
            private boolean satisfied;
            private List<Builder> parents = new ArrayList<>();
            private List<Builder> children = new ArrayList<>();

            public Result build() {
                if (this.built == null) {
                    this.propagate();

                    List<Result> builtChildren = new ArrayList<>();
                    this.built = new Result(this.bound, this.propagation, this.satisfied, Collections.emptyList(), Collections.unmodifiableList(builtChildren));
                    if (!this.parents.isEmpty()) {
                        this.built.setParents(this.parents.stream().map(Builder::build).toList());
                    }
                    this.children.forEach(b -> builtChildren.add(b.build()));
                }
                return this.built;
            }

            public Builder deepSetPropagation(Propagation propagation) {
                this.propagation = propagation;
                this.children.forEach(b -> b.deepSetPropagation(propagation));
                return this;
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
                this.propagate();
                return this.satisfied;
            }

            public Builder setSatisfied(boolean satisfied) {
                this.satisfied = satisfied;
                return this;
            }

            public List<Builder> parents() {
                return this.parents;
            }

            public Builder setParents(List<Builder> parents) {
                this.parents = parents;
                return this;
            }

            public Builder addParents(Builder... parents) {
                Collections.addAll(this.parents, parents);
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

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Builder builder = (Builder) o;
                return satisfied == builder.satisfied && propagation == builder.propagation && Objects.equals(bound, builder.bound) && Objects.equals(children, builder.children);
            }

            @Override
            public int hashCode() {
                return Objects.hash(propagation, bound, satisfied, children);
            }

            @Override
            public String toString() {
                return "TypeBound.Result.Builder{" +
                        "propagation=" + propagation +
                        ", bound=" + bound +
                        ", satisfied=" + satisfied +
                        ", children=" + children +
                        '}';
            }
        }

    }

}
