package honeyroasted.jype.fuzz;

public record Probe(int index, String targetHolder, String targetSource, FuzzExpr expression, boolean instance) {
    public static final String PARAMETERS = "String l0, int l1, java.util.List<String> l2, Object l3, int[] l4, Integer l5";

    public String declaration() {
        return "    " + (this.instance ? "" : "static ") + "void probe" + this.index + "(" + PARAMETERS + ") { " + this.targetSource + " x = " + this.expression.source() + "; }";
    }
}
