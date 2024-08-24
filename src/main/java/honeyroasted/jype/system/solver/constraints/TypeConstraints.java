package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface TypeConstraints {

    TypeMapper NO_OP = new TypeMapper(cn -> Function.identity());

    record TypeMapper(Function<ConstraintNode, Function<Type, Type>> mapper) {
    }

    final class Infer extends Constraint.Binary<MetaVarType, VarType> {
        public Infer(MetaVarType left, VarType right) {
            super(left, right);
        }

        @Override
        public String simpleName() {
            return "infer(" + this.left().simpleName() + " = " + this.right().simpleName() + ")";
        }

        @Override
        public String toString() {
            return this.left() + " = " + this.right() + " SHOULD BE INFERRED";
        }
    }

    final class Replaced extends Constraint.Binary<Type, Type> {
        public Replaced(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String simpleName() {
            return "infer(" + this.left().simpleName() + " = " + this.right().simpleName() + " )";
        }

        @Override
        public String toString() {
            return this.left() + " = " + this.right() + " SHOULD BE INFERRED";
        }
    }

    final class NarrowConstant extends Constraint.Binary<ExpressionInformation.Constant, Type> {
        public NarrowConstant(ExpressionInformation.Constant left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " FITS IN " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " <: " + this.right().simpleName();
        }
    }

    final class Equal extends Constraint.Binary<Type, Type> {
        public Equal(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " EQUALS " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " = " + this.right().simpleName();
        }
    }

    final class Instantiation extends Constraint.Binary<MetaVarType, Type> {

        public Instantiation(MetaVarType left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS INSTANTIATED AS " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " = " + this.right().simpleName();
        }
    }

    final class Compatible extends Constraint.Trinary<Type, Compatible.Context, Type> {

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

        public Compatible(Type left, Context middle, Type right) {
            super(left, middle, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS COMPATIBLE WITH " + this.right() + " IN " + this.middle();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " " + this.middle().symbol() + " " + this.right().simpleName();
        }

    }

    final class ExpressionCompatible extends Constraint.Trinary<ExpressionInformation, Compatible.Context, Type> {

        public ExpressionCompatible(ExpressionInformation left, Compatible.Context middle, Type right) {
            super(left, middle, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS COMPATIBLE WITH " + this.right() + " IN " + this.middle();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " " + this.middle().symbol() + " " + this.right().simpleName();
        }
    }

    final class Contains extends Constraint.Binary<Type, Type> {
        public Contains(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS CONTAINED BY " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " <= " + this.right().simpleName();
        }
    }

    final class LambdaThrows extends Constraint.Binary<ExpressionInformation.Lambda, Type> {

        public LambdaThrows(ExpressionInformation.Lambda left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " THROWS " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " throws(" + this.right().simpleName() + ")";
        }
    }

    final class InvocationReferenceThrows extends Constraint.Binary<ExpressionInformation.InvocationReference, Type> {

        public InvocationReferenceThrows(ExpressionInformation.InvocationReference left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " THROWS " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " throws(" + this.right().simpleName() + ")";
        }
    }

    final class InstantiationReferenceThrows extends Constraint.Binary<ExpressionInformation.InstantiationReference, Type> {

        public InstantiationReferenceThrows(ExpressionInformation.InstantiationReference left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " THROWS " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " throws(" + this.right().simpleName() + ")";
        }
    }

    final class Throws extends Constraint.Unary<MetaVarType> {
        public Throws(MetaVarType type) {
            super(type);
        }

        @Override
        public String toString() {
            return "THROWS " + this.value();
        }

        @Override
        public String simpleName() {
            return "throws(" + this.value().simpleName() + ")";
        }
    }

    class Capture extends Constraint.Binary<ParameterizedClassType, ParameterizedClassType> {
        public Capture(ParameterizedClassType left, ParameterizedClassType right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " CAPTURES " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " = capture(" + this.right().simpleName() + ")";
        }
    }

    class Subtype extends Constraint.Binary<Type, Type> {
        public Subtype(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS A SUBTYPE OF " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " <: " + this.right().simpleName();
        }
    }

    class TypeArgumentsMatch extends Constraint.Binary<ClassType, ClassType> {

        public TypeArgumentsMatch(ClassType left, ClassType right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return "<" + this.left().typeArguments().stream().map(Objects::toString).collect(Collectors.joining(", ")) + "> ARE COMPATIBLE WITH <" +
                    this.right().typeArguments().stream().map(Objects::toString).collect(Collectors.joining(", ")) + ">";
        }

        @Override
        public String simpleName() {
            return "<" + this.left().typeArguments().stream().map(Type::simpleName).collect(Collectors.joining(", ")) + "> ~= <" +
                    this.right().typeArguments().stream().map(Type::simpleName).collect(Collectors.joining(", ")) + ">";
        }
    }

    class GenericParameter extends Constraint.Binary<Type, Type> {
        public GenericParameter(Type left, Type right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return this.left() + " IS A COMPATIBLE GENERIC ARGUMENT WITH " + this.right();
        }

        @Override
        public String simpleName() {
            return this.left().simpleName() + " ~= " + this.right().simpleName();
        }
    }

}
