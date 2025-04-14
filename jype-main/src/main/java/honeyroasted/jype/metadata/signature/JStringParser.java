package honeyroasted.jype.metadata.signature;

import java.util.function.IntPredicate;

public class JStringParser {
    protected int index = 0;
    protected int[] codepoints;
    protected String str;

    public JStringParser(String str) {
        this.codepoints = str.codePoints().toArray();
        this.str = str;
    }

    protected int peek() {
        return this.codepoints[this.index];
    }

    protected int next() {
        if (!hasNext()) fail("Reached EOF unexpectedly");
        return this.codepoints[this.index++];
    }

    protected void skip(IntPredicate c) {
        if (hasNext() && c.test(peek())) {
            this.index++;
        }
    }

    protected void skip() {
        skip(c -> true);
    }

    protected void skip(char c) {
        skip(n -> c == n);
    }

    protected boolean hasNext() {
        return index < codepoints.length;
    }

    protected void readUntil(IntPredicate c, StringBuilder sb) {
        while (hasNext() && !c.test(peek())) {
            sb.appendCodePoint(next());
        }
    }

    protected String readUntil(IntPredicate c) {
        StringBuilder sb = new StringBuilder();
        readUntil(c, sb);
        return sb.toString();
    }

    protected void readWhile(IntPredicate c, StringBuilder sb) {
        while (hasNext() && c.test(peek())) {
            sb.appendCodePoint(next());
        }
    }

    protected String readWhile(IntPredicate c) {
        StringBuilder sb = new StringBuilder();
        readWhile(c, sb);
        return sb.toString();
    }

    protected <T> T expectEnd(T value) {
        return hasNext() ? fail("Expected EOF but got " + Character.toString(peek())) : value;
    }

    protected <T> T fail(String message) {
        throw new JStringParseException(message, this.index, this.str);
    }
}
