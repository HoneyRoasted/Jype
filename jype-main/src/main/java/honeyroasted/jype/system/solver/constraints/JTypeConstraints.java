package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.Constraint;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

public interface JTypeConstraints {
    final class Equal extends Constraint.Binary<JType, JType> {
        public Equal(JType left, JType right) {
            super(left, right);
        }

        public static Equal createBound(JType left, JType right) {
            if (left instanceof JMetaVarType mvt) {
                mvt.equalities().add(right);
            }

            if (right instanceof JMetaVarType mvt) {
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

    final class Subtype extends Constraint.Binary<JType, JType> {
        public Subtype(JType left, JType right) {
            super(left, right);
        }

        public static Subtype createBound(JType left, JType right) {
            if (left instanceof JMetaVarType mvt) {
                mvt.upperBounds().add(right);
            }

            if (right instanceof JMetaVarType mvt) {
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

    final class Infer extends Constraint.Binary<JMetaVarType, JVarType> {
        public Infer(JMetaVarType left, JVarType right) {
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

    final class Instantiation extends Constraint.Binary<JMetaVarType, JType> {

        public Instantiation(JMetaVarType left, JType right) {
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

    final class Compatible extends Constraint.Trinary<JType, Compatible.Context, JType> {

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

        public Compatible(JType left, Context middle, JType right) {
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

    final class ExpressionCompatible extends Constraint.Trinary<JExpressionInformation, Compatible.Context, JType> {

        public ExpressionCompatible(JExpressionInformation left, Compatible.Context middle, JType right) {
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

    final class Contains extends Constraint.Binary<JType, JType> {
        public Contains(JType left, JType right) {
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

    final class LambdaThrows extends Constraint.Binary<JExpressionInformation.Lambda, JType> {

        public LambdaThrows(JExpressionInformation.Lambda left, JType right) {
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

    final class InvocationReferenceThrows extends Constraint.Binary<JExpressionInformation.InvocationReference<?>, JType> {

        public InvocationReferenceThrows(JExpressionInformation.InvocationReference<?> left, JType right) {
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

    final class InstantiationReferenceThrows extends Constraint.Binary<JExpressionInformation.InstantiationReference, JType> {

        public InstantiationReferenceThrows(JExpressionInformation.InstantiationReference left, JType right) {
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

    final class Throws extends Constraint.Unary<JMetaVarType> {
        public Throws(JMetaVarType type) {
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

    class Capture extends Constraint.Binary<JParameterizedClassType, JParameterizedClassType> {
        public Capture(JParameterizedClassType left, JParameterizedClassType right) {
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

    class Contradiction extends Constraint.Binary<Constraint, Constraint> {

        public Contradiction(Constraint left, Constraint right) {
            super(left, right);
        }

        @Override
        public String simpleName() {
            return "(" + this.left().simpleName() + " & " + this.right().simpleName() + ") -> false";
        }

        @Override
        public String toString() {
            return this.left() + " AND " + this.right() + " IS A CONTRADICTION";
        }
    }
}
