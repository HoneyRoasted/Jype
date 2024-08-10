package honeyroasted.jype.type.signature;

public interface Signature {

    String value();

    record Type(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record Method(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

    record Class(String value) implements Signature {
        @Override
        public String toString() {
            return this.value;
        }
    }

}
