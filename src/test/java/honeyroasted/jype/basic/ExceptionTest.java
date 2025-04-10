package honeyroasted.jype.basic;

import honeyroasted.jype.metadata.signature.JStringParseException;

public class ExceptionTest {

    public static void main(String[] args) {
        throw new JStringParseException("Expected goodbye", 3, "Hello World");
    }

}
