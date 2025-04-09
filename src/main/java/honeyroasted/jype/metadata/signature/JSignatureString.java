package honeyroasted.jype.metadata.signature;

public sealed interface JSignatureString {

    String value();

    record Type(String value) implements JSignatureString {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record ClassDeclaration(String value) implements JSignatureString {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record MethodDeclaration(String value) implements JSignatureString {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record JMethodReference(String value) implements JSignatureString {
        @Override
        public String toString() {
            return this.value;
        }
    }

}
