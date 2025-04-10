package honeyroasted.jype.metadata.signature;

import java.util.ArrayList;
import java.util.List;

public class JDescriptorParser extends JStringParser {

    public JDescriptorParser(String str) {
        super(str);
    }

    public JDescriptor parse() {
        return readAmbiguous();
    }

    public JDescriptor.Type parseType() {
        return readType();
    }

    public JDescriptor.Method parseMethod() {
        return readMethod();
    }

    private JDescriptor readAmbiguous() {
        if (hasNext() && peek() == '(') {
            return readMethod();
        } else {
            return readType();
        }
    }

    private JDescriptor.Type readType() {
        if (hasNext() && peek() == '[') {
            return readArray();
        } else {
            return readNonArrayType();
        }
    }

    private JDescriptor.Type readNonArrayType() {
        if (!this.hasNext()) fail("Expected descriptor start (L, B, C, D, F, I, J, S, Z), got EOF");

        int typeInd = this.next();

        return switch (typeInd) {
            case 'L' -> {
                List<String> packageName = new ArrayList<>();
                String className = null;
                while (hasNext() && peek() != ';') {
                    String name = readUntil(c -> c == '/' || c == ';');
                    if (peek() == ';') {
                        className = name;
                    } else {
                        packageName.add(name);
                        skip('/');
                    }
                }
                skip(';');

                yield new JDescriptor.Class(packageName.isEmpty() ? JDescriptor.Class.DEFAULT_PACKAGE : packageName.toArray(String[]::new),
                        className);
            }
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> JDescriptor.Primitive.of(Character.toString(typeInd));
            default -> fail("Expected descriptor start (L, B, C, D, F, I, J, S, Z), got " + Character.toString(typeInd));
        };
    }

    private JDescriptor.Array readArray() {
        if (!this.hasNext()) fail("Expected array start ( [ ), got EOF");
        if (peek() != '[') fail("Expected array start ( [ ), got " + Character.toString(peek()));
        skip('[');
        return new JDescriptor.Array(readType());
    }

    private JDescriptor.Method readMethod() {
        if (!this.hasNext()) fail("Expected method start ( ( ), got EOF");
        if (peek() != '(') fail("Expected array start ( ( ), got " + Character.toString(peek()));
        skip('(');

        List<JDescriptor.Type> params = new ArrayList<>();
        while (hasNext() && peek() != ')') {
            params.add(readType());
        }
        skip(')');

        JDescriptor.Type ret = readType();
        return new JDescriptor.Method(ret, params);
    }

}
