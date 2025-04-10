package honeyroasted.jype.metadata.signature;

public class JStringParseException extends RuntimeException {
    private int index;
    private String signature;
    private String message;

    public JStringParseException(String message, int index, String signature) {
        super(buildMessage(message, signature, index));
        this.message = message;
        this.index = index;
        this.signature = signature;
    }

    public JStringParseException(String message, Throwable cause, int index, String signature) {
        super(buildMessage(message, signature, index), cause);
        this.message = message;
        this.index = index;
        this.signature = signature;
    }

    public JStringParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int index, String signature) {
        super(buildMessage(message, signature, index), cause, enableSuppression, writableStackTrace);
        this.message = message;
        this.index = index;
        this.signature = signature;
    }

    public int index() {
        return index;
    }

    public String signature() {
        return signature;
    }

    public String rawMessage() {
        return this.message;
    }

    private static String buildMessage(String message, String sig, int index) {
        return "\n" + message + ":\n" + sig + "\n" + "-".repeat(index) + "^";
    }
}
