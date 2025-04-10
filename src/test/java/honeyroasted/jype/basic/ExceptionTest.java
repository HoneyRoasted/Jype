package honeyroasted.jype.basic;

import honeyroasted.jype.metadata.signature.JSignatureParseException;

public class ExceptionTest {

    public static void main(String[] args) {
        throw new JSignatureParseException("Expected goodbye", 3, "Hello World");
    }

}
