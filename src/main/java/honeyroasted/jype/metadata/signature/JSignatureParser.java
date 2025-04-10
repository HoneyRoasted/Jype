package honeyroasted.jype.metadata.signature;

public class JSignatureParser {
    private int index = 0;
    private int[] codepoints;
    private String sig;

    public JSignatureParser(String sig) {
        this.codepoints = sig.codePoints().toArray();
        this.sig = sig;
    }

    private int peek() {
        return this.codepoints[this.index];
    }

    private int next() {
        return this.codepoints[this.index++];
    }

    private boolean hasNext() {
        return index < codepoints.length;
    }

    private JSignature.Type readDescriptor() {
        if (!this.hasNext()) fail("Expected descriptor, got EOF");

        int typeInd = this.next();

        return switch (typeInd) {
            case 'L' -> {
                StringBuilder sb = new StringBuilder();
                sb.append("L");
                while ((Character.isAlphabetic(peek()) || peek() == '/') && hasNext()) {
                    sb.appendCodePoint(next());
                }

                if (peek() == ';') next();

                yield new JSignature.Type(sb.toString());
            }
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> new JSignature.Type(Character.toString(typeInd));
            default -> fail("Unknown type indicator '" + Character.toString(typeInd) + "' expected one of [L, B, C, D, F, I, J, S, Z]");
        };
    }

    private <T> T fail(String message) {
        throw new JSignatureParseException(message, this.index, this.sig);
    }

}
