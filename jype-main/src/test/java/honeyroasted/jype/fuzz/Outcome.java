package honeyroasted.jype.fuzz;

public record Outcome<T>(T value, boolean timedOut, Throwable error) {
    public static <T> Outcome<T> value(T value) {
        return new Outcome<>(value, false, null);
    }

    public static <T> Outcome<T> timeout() {
        return new Outcome<>(null, true, null);
    }

    public static <T> Outcome<T> error(Throwable error) {
        return new Outcome<>(null, false, error);
    }

    public String render() {
        if (this.timedOut) {
            return "TIMEOUT";
        } else if (this.error != null) {
            StackTraceElement top = this.error.getStackTrace().length == 0 ? null : this.error.getStackTrace()[0];
            return "ERROR " + this.error + (top == null ? "" : " at " + top);
        }
        return String.valueOf(this.value);
    }
}
