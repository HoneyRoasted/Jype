package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.Constraint;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

public interface TypeConstraints {
    final class Equal extends Constraint.Binary<Type, Type> {
        public Equal(Type left, Type right) {
            super(left, right);
        }

        public static Equal createBound(Type left, Type right) {
            if (left instanceof MetaVarType mvt) {
                mvt.equalities().add(right);
            }

            if (right instanceof MetaVarType mvt) {
                mvt.equalities().add(left);
            }
            return new Equal(left, right);
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

    final class Subtype extends Constraint.Binary<Type, Type> {
        public Subtype(Type left, Type right) {
            super(left, right);
        }

        public static Subtype createBound(Type left, Type right) {
            if (left instanceof MetaVarType mvt) {
                mvt.upperBounds().add(right);
            }

            if (right instanceof MetaVarType mvt) {
                mvt.lowerBounds().add(left);
            }

            return new Subtype(left, right);
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
}
