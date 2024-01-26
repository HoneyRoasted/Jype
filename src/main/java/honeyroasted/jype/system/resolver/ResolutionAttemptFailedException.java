package honeyroasted.jype.system.resolver;

public class ResolutionAttemptFailedException extends RuntimeException {
    public ResolutionAttemptFailedException() {
    }

    public ResolutionAttemptFailedException(String message) {
        super(message);
    }

    public ResolutionAttemptFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResolutionAttemptFailedException(Throwable cause) {
        super(cause);
    }
}
