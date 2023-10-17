package honeyroasted.jype.type;

public interface Signature {

    String value();

    record Type(String value) implements Signature {}
    record Method(String value) implements Signature {}
    record Class(String value) implements Signature {}

}
