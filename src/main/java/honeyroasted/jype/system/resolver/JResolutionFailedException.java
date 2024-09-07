package honeyroasted.jype.system.resolver;

public class JResolutionFailedException extends Exception {
    public JResolutionFailedException() {
    }

    public JResolutionFailedException(String message) {
        super(message);
    }

    public JResolutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public JResolutionFailedException(Throwable cause) {
        super(cause);
    }

}
