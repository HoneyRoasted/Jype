package honeyroasted.jype.system.resolver.reflection;

public class JReflectionLookupException extends RuntimeException {

    public JReflectionLookupException() {
    }

    public JReflectionLookupException(String message) {
        super(message);
    }

    public JReflectionLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    public JReflectionLookupException(Throwable cause) {
        super(cause);
    }

    public JReflectionLookupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
