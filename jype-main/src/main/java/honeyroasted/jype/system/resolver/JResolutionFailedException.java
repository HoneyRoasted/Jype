package honeyroasted.jype.system.resolver;

public class JResolutionFailedException extends RuntimeException {
    private JResolutionResult<?, ?> result;

    public JResolutionFailedException(JResolutionResult<?, ?> result) {
        this.result = result;
    }

    public JResolutionFailedException(String message, JResolutionResult<?, ?> result) {
        super(message);
        this.result = result;
    }

    public JResolutionFailedException(String message, Throwable cause, JResolutionResult<?, ?> result) {
        super(message, cause);
        this.result = result;
    }

    public JResolutionFailedException(Throwable cause, JResolutionResult<?, ?> result) {
        super(cause);
        this.result = result;
    }

    public JResolutionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, JResolutionResult<?, ?> result) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.result = result;
    }

    public JResolutionResult<?, ?> result() {
        return this.result;
    }
}
