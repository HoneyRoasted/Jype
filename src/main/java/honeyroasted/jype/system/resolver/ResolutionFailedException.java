package honeyroasted.jype.system.resolver;

public class ResolutionFailedException extends Exception {
    public ResolutionFailedException() {
    }

    public ResolutionFailedException(String message) {
        super(message);
    }

    public ResolutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResolutionFailedException(Throwable cause) {
        super(cause);
    }

}
