package honeyroasted.jype.type;

public interface Signature {

    String value();

    record Type(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record MethodDeclaration(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record MethodReference(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record ClassDeclaration(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

}
