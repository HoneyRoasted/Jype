package honeyroasted.jype.system.resolver.binary;

public class JBinaryLookupException extends RuntimeException {
    public JBinaryLookupException() {
    }

    public JBinaryLookupException(String message) {
        super(message);
    }

    public JBinaryLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    public JBinaryLookupException(Throwable cause) {
        super(cause);
    }

    public JBinaryLookupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
