package honeyroasted.jype.fuzz;

public record FuzzMismatch(String category, String description, String javac, String jype, String context) {
    public String render() {
        return "[" + this.category + "] " + this.description + "\n    javac: " + this.javac + "\n    jype:  " + this.jype + (this.context == null ? "" : "\n" + this.context);
    }
}
