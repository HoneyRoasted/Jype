package honeyroasted.jype.metadata.signature;

import honeyroasted.jype.metadata.location.JClassLocation;

import java.util.List;
import java.util.stream.Collectors;

public interface JDescriptor {

    interface Type extends JDescriptor {

        JClassLocation toLocation();

    }

    enum Primitive implements Type {
        VOID("V"),
        BOOLEAN("Z"),
        BYTE("B"),
        SHORT("S"),
        CHAR("C"),
        INT("I"),
        LONG("J"),
        FLOAT("F"),
        DOUBLE("D");

        private String descriptor;
        Primitive(String descriptor) {
            this.descriptor = descriptor;
        }


        @Override
        public String toString() {
            return this.descriptor;
        }

        public static Primitive of(String descriptor) {
            for (Primitive prim : Primitive.values()) {
                if (prim.descriptor.equals(descriptor)) {
                    return prim;
                }
            }
            throw new IllegalArgumentException("Unknown primitive descriptor: " + descriptor);
        }

        @Override
        public JClassLocation toLocation() {
            return JClassLocation.ofDescriptor(this.descriptor);
        }
    }

    record Class(String[] packageName, String name) implements Type {
        public static final String[] DEFAULT_PACKAGE = new String[0];

        public Class(String name) {
            this(DEFAULT_PACKAGE, name);
        }

        @Override
        public String toString() {
            return "L" + String.join("/", this.packageName)
                    + (this.packageName.length != 0 ? "/" : "")
                    + this.name + ";";
        }

        @Override
        public JClassLocation toLocation() {
            return JClassLocation.of(packageName, name);
        }
    }

    record Array(Type component) implements Type {
        @Override
        public String toString() {
            return "[" + this.component;
        }

        @Override
        public JClassLocation toLocation() {
            return new JClassLocation(JClassLocation.Type.ARRAY, this.component.toLocation(), "[]");
        }
    }

    record Method(Type ret, List<Type> parameters) implements JDescriptor {
        @Override
        public String toString() {
            return "(" + parameters.stream().map(Object::toString).collect(Collectors.joining()) + ")" + ret;
        }
    }

}
