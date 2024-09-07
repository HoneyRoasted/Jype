package honeyroasted.jype.type;

public interface JSignature {

    String value();

    record JType(String value) implements JSignature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record MethodDeclaration(String value) implements JSignature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record JMethodReference(String value) implements JSignature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record ClassDeclaration(String value) implements JSignature {
        @Override
        public String toString() {
            return this.value;
        }
    }

}
