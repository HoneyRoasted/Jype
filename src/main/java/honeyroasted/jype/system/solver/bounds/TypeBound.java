package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TypeBound {

    enum Classification {
        BOUND,
        CONSTRAINT
    }

    List<?> parameters();

    TypeBound createNew(List<Object> parameters);

    String simpleName();

    class False implements TypeBound {

        public static final False INSTANCE = new False();

        private False() {
        }

        @Override
        public List<?> parameters() {
            return Collections.emptyList();
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return INSTANCE;
        }

        @Override
        public String simpleName() {
            return "false";
        }

        @Override
        public String toString() {
            return "false";
        }
    }

    abstract class Unary<T> implements TypeBound {
        protected T value;

        public Unary(T value) {
            this.value = value;
        }

        public T value() {
            return this.value;
        }

        @Override
        public List<?> parameters() {
            return List.of(this.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) return false;
            Unary<?> other = (Unary<?>) obj;
            return Objects.equals(this.value, other.value);
        }
    }

    abstract class Binary<L, R> implements TypeBound {
        protected L left;
        protected R right;

        public Binary(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public boolean hasMetaVar() {
            return this.left instanceof MetaVarType || this.right instanceof MetaVarType;
        }

        public Optional<MetaVarType> getMetaVar() {
            return this.left instanceof MetaVarType mvt ? Optional.of(mvt) :
                    this.right instanceof MetaVarType mvt ? Optional.of(mvt) :
                            Optional.empty();
        }

        public Optional<Type> getOtherType() {
            return this.left instanceof MetaVarType ?
                    (this.right instanceof Type t ? Optional.of(t) : Optional.empty()) :
                    (this.left instanceof Type t ? Optional.of(t) : Optional.empty());
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

    final class Infer extends Unary<VarType> {

        public Infer(VarType value) {
            super(value);
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Infer((VarType) parameters.get(0));
        }

        @Override
        public String simpleName() {
            return "infer(" + this.value.simpleName() + ")";
        }

        @Override
        public String toString() {
            return this.value + " SHOULD BE INFERRED";
        }
    }

    final class NarrowConstant extends Binary<ExpressionInformation.Constant, Type> {

        public NarrowConstant(ExpressionInformation.Constant left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " FITS IN " + this.right;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new NarrowConstant((ExpressionInformation.Constant) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " <: " + this.right.simpleName();
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
        public TypeBound createNew(List<Object> parameters) {
            return new Equal((Type) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " = " + this.right.simpleName();
        }
    }

    final class Instantiation extends Binary<MetaVarType, Type> {

        public Instantiation(MetaVarType left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS INSTANTIATED AS " + this.right;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Instantiation((MetaVarType) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " = " + this.right.simpleName();
        }
    }

    final class Compatible extends Binary<Type, Type> {
        public enum Context {
            ASSIGNMENT("~<:"),
            STRICT_INVOCATION("<:"),
            LOOSE_INVOCATION("~<:"),
            EXPLICIT_CAST("~:>");

            private final String symbol;

            Context(String symbol) {
                this.symbol = symbol;
            }

            public String symbol() {
                return this.symbol;
            }
        }

        private Context context;

        public Compatible(Type left, Type right) {
            this(left, right, Context.LOOSE_INVOCATION);
        }

        public Compatible(Type left, Type right, Context context) {
            super(left, right);
            this.context = context;
        }

        public Context context() {
            return this.context;
        }

        @Override
        public String toString() {
            return this.left + " IS COMPATIBLE WITH " + this.right + " IN " + this.context;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Compatible((Type) parameters.get(0), (Type) parameters.get(1), (Context) parameters.get(2));
        }

        @Override
        public List<Object> parameters() {
            return List.of(this.left, this.right, this.context);
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " " + this.context.symbol() + " " + this.right.simpleName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Compatible that = (Compatible) o;
            return context == that.context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), context);
        }
    }

    final class ExpressionCompatible extends Binary<ExpressionInformation, Type> {
        private Compatible.Context context;

        public ExpressionCompatible(ExpressionInformation left, Type right) {
            this(left, right, Compatible.Context.LOOSE_INVOCATION);
        }

        public ExpressionCompatible(ExpressionInformation left, Type right, Compatible.Context context) {
            super(left, right);
            this.context = context;
        }

        public Compatible.Context context() {
            return this.context;
        }

        @Override
        public String toString() {
            return this.left + " IS COMPATIBLE WITH " + this.right + " IN " + this.context;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new ExpressionCompatible((ExpressionInformation) parameters.get(0), (Type) parameters.get(1), (Compatible.Context) parameters.get(2));
        }

        @Override
        public List<Object> parameters() {
            return List.of(this.left, this.right, this.context);
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " " + this.context.symbol() + " " + this.right.simpleName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            ExpressionCompatible that = (ExpressionCompatible) o;
            return context == that.context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), context);
        }
    }

    final class Contains extends Binary<Type, Type> {
        public Contains(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " IS CONTAINED BY " + this.right;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Contains((Type) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " <= " + this.right.simpleName();
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
        public TypeBound createNew(List<Object> parameters) {
            return new LambdaThrows((ExpressionInformation) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " throws(" + this.right.simpleName() + ")";
        }
    }

    final class Throws extends Unary<Type> {
        public Throws(Type type) {
            super(type);
        }

        @Override
        public String toString() {
            return "THROWS " + this.value;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Throws((Type) parameters.get(0));
        }

        @Override
        public String simpleName() {
            return "throws(" + this.value.simpleName() + ")";
        }
    }

    class Capture extends Binary<ParameterizedClassType, ParameterizedClassType> {
        public Capture(ParameterizedClassType left, ParameterizedClassType right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left + " CAPTURES " + this.right;
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new Capture((ParameterizedClassType) parameters.get(0), (ParameterizedClassType) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " = capture(" + this.right.simpleName() + ")";
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
        public TypeBound createNew(List<Object> parameters) {
            return new Subtype((Type) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " <: " + this.right.simpleName();
        }
    }

    class TypeArgumentsMatch extends Binary<ClassType, ClassType> {

        public TypeArgumentsMatch(ClassType left, ClassType right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return "<" + this.left.typeArguments().stream().map(Objects::toString).collect(Collectors.joining(", ")) + "> ARE COMPATIBLE WITH <" +
                    this.right.typeArguments().stream().map(Objects::toString).collect(Collectors.joining(", ")) + ">";
        }

        @Override
        public TypeBound createNew(List<Object> parameters) {
            return new TypeArgumentsMatch((ClassType) parameters.get(0), (ClassType) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return "<" + this.left.typeArguments().stream().map(Type::simpleName).collect(Collectors.joining(", ")) + "> ~= <" +
                    this.right.typeArguments().stream().map(Type::simpleName).collect(Collectors.joining(", ")) + ">";
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
        public TypeBound createNew(List<Object> parameters) {
            return new GenericParameter((Type) parameters.get(0), (Type) parameters.get(1));
        }

        @Override
        public String simpleName() {
            return this.left.simpleName() + " ~= " + this.right.simpleName();
        }
    }

    interface ResultView {
        boolean satisfied();

        TypeBound bound();

        Result.Propagation propagation();

        List<? extends ResultView> parents();

        List<? extends ResultView> children();

        Result.Builder toBuilder();
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

        @Override
        public Builder toBuilder() {
            return this.toBuilder(new IdentityHashMap<>());
        }

        private Builder toBuilder(Map<Result, Builder> createdMap) {
            if (createdMap.containsKey(this)) {
                return createdMap.get(this);
            }

            Builder builder = builder(this.bound);
            createdMap.put(this, builder);
            builder.setPropagation(this.propagation)
                    .setSatisfied(this.satisfied)
                    .setChildren(this.children.stream().map(r -> r.toBuilder(createdMap)).collect(Collectors.toCollection(ArrayList::new)))
                    .setParents(this.parents.stream().map(r -> r.toBuilder(createdMap)).collect(Collectors.toCollection(ArrayList::new)));
            return builder;
        }

        public static Builder builder(TypeBound bound) {
            return new Builder().setBound(bound);
        }

        private void toString(List<String> building, boolean useSimpleName) {
            String ind = "    ";
            building.add("Condition: " + (useSimpleName ? this.bound.simpleName() : this.bound.toString()));
            building.add("Satisfied: " + this.satisfied);

            if (!this.children.isEmpty()) {
                building.add("Propagation: " + this.propagation);
                building.add("Children: " + this.children.size());

                List<String> children = new ArrayList<>();
                for (int i = 0; i < this.children.size(); i++) {
                    this.children.get(i).toString(children, useSimpleName);
                    if (i < this.children.size() - 1) {
                        children.add("");
                    }
                }

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

        public String toString(boolean useSimpleName) {
            List<String> building = new ArrayList<>();
            this.toString(building, useSimpleName);
            return String.join("\n", building);
        }

        @Override
        public String toString() {
            return this.toString(false);
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

        public static Builder builder(TypeBound bound, Collection<Builder> originators) {
            Builder builder = new Builder().setBound(bound).addParents(originators);
            for (Builder originator : originators) {
                originator.addChildren(builder);
            }
            return builder;
        }

        public static Builder builder(TypeBound bound, Propagation propagation, Collection<Builder> originators) {
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
            NOR,
            INHERIT
        }

        public enum Trinary {
            UNKNOWN(false), TRUE(true), FALSE(false);

            private boolean boolVal;

            Trinary(boolean boolVal) {
                this.boolVal = boolVal;
            }

            public boolean toBool() {
                return this.boolVal;
            }

            public Trinary not() {
                return this == UNKNOWN ? UNKNOWN : of(!this.boolVal);
            }

            public Trinary or(Trinary other) {
                return this == UNKNOWN || this == FALSE ? other : this;
            }

            public Trinary and(Trinary other) {
                return other == UNKNOWN || other == FALSE ? other : this;
            }

            public static Trinary of(boolean val) {
                return val ? TRUE : FALSE;
            }
        }

        public static class Builder implements ResultView {
            private Builder delegate;

            private Result built;
            private Propagation propagation = Propagation.NONE;
            private TypeBound bound;
            private Trinary satisfied = Trinary.UNKNOWN;
            private List<Builder> parents = new ArrayList<>();
            private List<Builder> children = new ArrayList<>();

            public void replaceWith(TypeBound.Result.Builder other) {
                if (this.delegate != null) {
                    this.delegate.replaceWith(other);
                } else {
                    this.delegate = other;
                }
            }

            public Result build() {
                if (this.delegate != null) return this.delegate.build();

                if (this.built == null) {
                    this.propagate();

                    List<Result> builtChildren = new ArrayList<>();
                    this.built = new Result(this.bound, this.children.isEmpty() ? Propagation.NONE :
                            this.children.size() == 1 ? Propagation.INHERIT : this.propagation, this.satisfied.toBool(), Collections.emptyList(), Collections.unmodifiableList(builtChildren));
                    if (!this.parents.isEmpty()) {
                        this.built.setParents(this.parents.stream().map(Builder::build).toList());
                    }
                    this.children.forEach(b -> builtChildren.add(b.build()));
                }
                return this.built;
            }

            public Builder deepSetPropagation(Propagation propagation) {
                if (this.delegate != null) {
                    this.delegate.deepSetPropagation(propagation);
                    return this;
                }

                this.propagation = propagation;
                this.children.forEach(b -> b.deepSetPropagation(propagation));
                return this;
            }

            public Builder propagate() {
                if (this.delegate != null) {
                    this.delegate.propagate();
                    return this;
                }

                if (!this.children.isEmpty()) {
                    this.children.forEach(Builder::propagate);
                    switch (this.propagation) {
                        case AND, INHERIT -> this.andChildren();
                        case OR -> this.orChildren();
                        case NAND -> this.andChildren().not();
                        case NOR -> this.orChildren().not();
                        case NONE -> {
                            if (this.children.size() == 1) {
                                this.setSatisfied(this.children.get(0).satisfied());
                            }
                        }
                    }
                }
                return this;
            }

            public Builder not() {
                if (this.delegate != null) {
                    this.delegate.not();
                    return this;
                }

                this.satisfied = this.satisfied.not();
                return this;
            }

            public Builder andChildren() {
                if (this.delegate != null) {
                    this.delegate.andChildren();
                    return this;
                }

                this.satisfied = Trinary.of(this.children.stream().allMatch(Builder::satisfied));
                return this;
            }

            public Builder orChildren() {
                if (this.delegate != null) {
                    this.delegate.orChildren();
                    return this;
                }

                this.satisfied = Trinary.of(this.children.stream().anyMatch(Builder::satisfied));
                return this;
            }

            public TypeBound bound() {
                if (this.delegate != null) {
                    return this.delegate.bound();
                }

                return this.bound;
            }

            public Builder setBound(TypeBound bound) {
                if (this.delegate != null) {
                    this.delegate.setBound(bound);
                    return this;
                }

                this.bound = bound;
                return this;
            }

            public boolean satisfied() {
                if (this.delegate != null) {
                    return this.delegate.satisfied();
                }

                this.propagate();
                return this.satisfied.toBool();
            }

            public Trinary getSatisfied() {
                if (this.delegate != null) {
                    return this.delegate.getSatisfied();
                }

                return this.satisfied;
            }

            public Builder setSatisfied(boolean satisfied) {
                if (this.delegate != null) {
                    this.delegate.setSatisfied(satisfied);
                    return this;
                }

                this.satisfied = Trinary.of(satisfied);
                return this;
            }

            public List<Builder> parents() {
                if (this.delegate != null) {
                    return this.delegate.parents();
                }

                return this.parents;
            }

            public Builder setParents(List<Builder> parents) {
                if (this.delegate != null) {
                    this.delegate.setParents(parents);
                    return this;
                }

                this.parents = parents;
                return this;
            }

            public Builder addParents(Builder... parents) {
                if (this.delegate != null) {
                    this.delegate.addParents(parents);
                    return this;
                }

                Collections.addAll(this.parents, parents);
                return this;
            }

            public Builder addParents(Collection<Builder> parents) {
                if (this.delegate != null) {
                    this.delegate.addParents(parents);
                    return this;
                }

                this.parents.addAll(parents);
                return this;
            }

            public List<Builder> children() {
                if (this.delegate != null) {
                    return this.delegate.children();
                }

                return this.children;
            }

            @Override
            public Builder toBuilder() {
                return this;
            }

            public Builder setChildren(List<Builder> children) {
                if (this.delegate != null) {
                    this.delegate.setChildren(children);
                    return this;
                }

                this.children = children;
                return this;
            }

            public Builder addChildren(Builder... children) {
                if (this.delegate != null) {
                    this.delegate.addChildren(children);
                    return this;
                }

                Collections.addAll(this.children, children);
                return this;
            }

            public Builder addChildren(Collection<Builder> children) {
                if (this.delegate != null) {
                    this.delegate.addChildren(children);
                    return this;
                }

                this.children.addAll(children);
                return this;
            }

            public Propagation propagation() {
                if (this.delegate != null) {
                    return this.delegate.propagation();
                }

                return this.propagation;
            }

            public Builder setPropagation(Propagation propagation) {
                if (this.delegate != null) {
                    this.delegate.setPropagation(propagation);
                    return this;
                }

                this.propagation = propagation;
                return this;
            }

            public boolean isDelegated() {
                return this.delegate != null;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Builder builder = (Builder) o;

                if (this.delegate != null) {
                    return this.delegate.equals(builder);
                }

                return satisfied == builder.satisfied && propagation == builder.propagation && Objects.equals(bound, builder.bound) && Objects.equals(children, builder.children);
            }

            @Override
            public int hashCode() {
                if (this.delegate != null) {
                    return this.delegate.hashCode();
                }

                return Objects.hash(propagation, bound, satisfied, children);
            }

            @Override
            public String toString() {
                if (this.delegate != null) {
                    return this.delegate.toString();
                }

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
