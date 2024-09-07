package honeyroasted.jype.system.resolver;

public class JResolutionAttemptFailedException extends RuntimeException {
    public JResolutionAttemptFailedException() {
    }

    public JResolutionAttemptFailedException(String message) {
        super(message);
    }

    public JResolutionAttemptFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public JResolutionAttemptFailedException(Throwable cause) {
        super(cause);
    }
}
