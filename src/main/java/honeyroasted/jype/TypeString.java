package honeyroasted.jype;

import java.util.Optional;

public interface TypeString {

    String value();

    boolean successful();

    static TypeString successful(String value) {
        return new Successful(value);
    }

    static TypeString failure(String type, String target) {
        return new Failure(type, target);
    }

    enum Context {
        DECLARATION,
        CONCRETE
    }

    class Successful implements TypeString {
        private String value;

        public Successful(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return this.value;
        }

        @Override
        public boolean successful() {
            return true;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    class Failure implements TypeString {
        private String type;
        private String target;

        public Failure(String type, String target) {
            this.type = type;
            this.target = target;
        }

        @Override
        public String value() {
            return null;
        }

        @Override
        public boolean successful() {
            return false;
        }

        public String type() {
            return type;
        }

        public String target() {
            return target;
        }

        @Override
        public String toString() {
            return "<unable to convert type " + this.target + " to " + this.type + ">";
        }
    }

}
