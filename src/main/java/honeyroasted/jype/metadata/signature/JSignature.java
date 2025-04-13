package honeyroasted.jype.metadata.signature;

import honeyroasted.jype.type.JGenericDeclaration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public sealed interface JSignature {

    sealed interface InformalType extends JSignature {

    }

    sealed interface Declaration extends JSignature {

    }

    sealed interface GenericDeclaration extends Declaration {
        List<VarTypeDeclaration> vars();
    }

    record Array(InformalType component) implements InformalType {
        @Override
        public String toString() {
            return "[" + this.component;
        }
    }

    record Type(JDescriptor.Type descriptor) implements InformalType {
        @Override
        public String toString() {
            return this.descriptor.toString();
        }

        public boolean isPrimitive() {
            return descriptor instanceof JDescriptor.Primitive;
        }

        public Parameterized asParameterized() {
            return new Parameterized(this, Collections.emptyList());
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
                sb.append(this.type.descriptor() instanceof JDescriptor.Class jdc ? jdc.name() : this.type);
            } else {
                sb.append(cutEnd(type));
            }



            if (!this.parameters.isEmpty()) {
                sb.append("<");
                this.parameters.forEach(sb::append);
                sb.append(">");
            }

            return sb.append(type.isPrimitive() ? "" : ";").toString();
        }
    }

    record VarType(String name) implements InformalType {
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

    record IntersectionType(List<InformalType> types) implements InformalType {
        @Override
        public String toString() {
            return types.stream().map(Object::toString).collect(Collectors.joining(":"));
        }
    }

    record VarTypeDeclaration(String name, InformalType classBound, List<InformalType> interfaceBounds) implements Declaration {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.name).append(":");

            if (this.classBound != null){
                sb.append(this.classBound);
            }

            if (!this.interfaceBounds.isEmpty()) {
                this.interfaceBounds.forEach(it -> sb.append(":").append(it));
            }
            return sb.toString();
        }
    }

    record ClassDeclaration(List<VarTypeDeclaration> vars, InformalType superclass, List<InformalType> interfaces) implements GenericDeclaration {
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

    record MethodDeclaration(List<VarTypeDeclaration> vars, List<InformalType> parameters, InformalType returnType, List<InformalType> exceptions) implements GenericDeclaration {
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

    record Declared(InformalType signature, JGenericDeclaration containing) {
        @Override
        public String toString() {
            return this.signature + " IN " + this.containing;
        }
    }

    private static String cutEnd(JSignature sig) {
        return cutEnd(String.valueOf(sig));
    }

    private static String cutEnd(String str) {
        if (str.endsWith(";")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }

    private static String cutStart(JSignature sig) {
        return cutStart(String.valueOf(sig));
    }

    private static String cutStart(String str) {
        if (str.startsWith("L")) {
            return str.substring(1);
        }
        return str;
    }
}
