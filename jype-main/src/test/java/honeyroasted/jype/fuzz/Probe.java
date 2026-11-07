package honeyroasted.jype.fuzz;

public record Probe(int index, String targetHolder, String targetSource, FuzzExpr expression) {
    public String declaration() {
        return "    static void probe" + this.index + "() { " + this.targetSource + " x = " + this.expression.source() + "; }";
    }
}
