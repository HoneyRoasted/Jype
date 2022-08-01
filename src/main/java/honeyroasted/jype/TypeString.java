package honeyroasted.jype;

import java.util.Optional;

public interface TypeString {

    String value();

    boolean successful();

    static TypeString successful(String value) {
        return new Successful(value);
    }

    static TypeString failure(Class<?> type, Target target) {
        return new Failure(type, target);
    }

    enum Context {
        DECLARATION,
        CONCRETE
    }

    enum Target {
        SOURCE,
        DESCRIPTOR,
        SIGNATURE
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
        private Class<?> type;
        private Target target;

        public Failure(Class<?> type, Target target) {
            this.type = type;
            this.target = target;
        }

        @Override
        public String value() {
            return toString();
        }

        @Override
        public boolean successful() {
            return false;
        }

        public Class<?> type() {
            return type;
        }

        public Target target() {
            return target;
        }

        @Override
        public String toString() {
            return "<unable to convert type " + this.target + " to " + this.type + ">";
        }
    }

}
