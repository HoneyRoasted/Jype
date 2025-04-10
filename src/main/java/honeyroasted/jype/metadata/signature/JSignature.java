package honeyroasted.jype.metadata.signature;

import java.util.List;

public interface JSignature {

    interface InformalType extends JSignature {

    }

    record Array(InformalType element) implements InformalType {
        @Override
        public String toString() {
            return "[" + this.element;
        }
    }

    record Type(String descriptor) implements InformalType {
        @Override
        public String toString() {
            return this.descriptor;
        }

        public boolean isPrimitive() {
            return descriptor.length() == 1;
        }
    }

    record Parameterized(Parameterized outer, Type type, List<InformalType> parameters) implements InformalType {

        public Parameterized(Type type, List<InformalType> parameters) {
            this(null, type, parameters);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (this.outer != null) {
                sb.append(cutEnd(this.outer)).append(".");
            }

            sb.append(cutEnd(type));

            if (!this.parameters.isEmpty()) {
                sb.append("<");
                this.parameters.forEach(sb::append);
                sb.append(">");
            }

            return sb.append(type.isPrimitive() ? "" : ";").toString();
        }
    }

    record TypeVar(String name, InformalType bound, List<InformalType> bounds) implements JSignature {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.name).append(":").append(this.bound);
            if (!this.bounds.isEmpty()) {
                this.bounds.forEach(it -> sb.append(":").append(it));
            }
            return sb.toString();
        }
    }

    record TypeVarReference(String name) implements InformalType {
        @Override
        public String toString() {
            return "T" + this.name + ";";
        }
    }

    record WildType(InformalType upper, InformalType lower) implements InformalType {
        @Override
        public String toString() {
            if (this.upper != null) {
                return "+" + this.upper;
            } else if (this.lower != null) {
                return "-" + this.lower;
            } else {
                return "*";
            }
        }
    }

    record ClassDeclaration(List<TypeVar> vars, InformalType superclass, List<InformalType> interfaces) implements JSignature {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!this.vars.isEmpty()) {
                sb.append("<");
                this.vars.forEach(sb::append);
                sb.append(">");
            }

            sb.append(superclass);
            this.interfaces.forEach(sb::append);

            return sb.toString();
        }
    }

    record MethodDeclaration(List<TypeVar> vars, List<InformalType> parameters, InformalType returnType, List<InformalType> exceptions) implements JSignature {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!this.vars.isEmpty()) {
                sb.append("<");
                vars.forEach(sb::append);
                sb.append(">");
            }

            sb.append("(");
            parameters.forEach(sb::append);
            sb.append(")").append(returnType);

            exceptions.forEach(it -> sb.append("^").append(it));

            return sb.toString();
        }
    }

    private static String cutEnd(JSignature sig) {
        String str = String.valueOf(sig);

        if (str.endsWith(";")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }
}
