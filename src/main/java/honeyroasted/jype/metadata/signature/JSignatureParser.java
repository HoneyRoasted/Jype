package honeyroasted.jype.metadata.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JSignatureParser extends JStringParser {

    public JSignatureParser(String str) {
        super(str);
    }

    public JSignature.Declaration parseDeclaration() {
        return expectEnd(readAmbiguousDeclaration());
    }

    public JSignature.MethodDeclaration parseMethodDeclaration() {
        return expectEnd(readMethodDeclaration());
    }

    public JSignature.ClassDeclaration parseClassDeclaration() {
        return expectEnd(readClassDeclaration());
    }

    public JSignature.InformalType parseInformalType() {
        return expectEnd(readInformal());
    }

    private JSignature.Declaration readAmbiguousDeclaration() {
        List<JSignature.VarTypeDeclaration> declarations = isParametersNext() ? readVarDeclarations() : Collections.emptyList();
        if (hasNext() && next() == '(') {
            List<JSignature.InformalType> parameters = new ArrayList<>();
            skip('(');
            while (hasNext() && peek() != ')') {
                parameters.add(readInformal());
            }
            skip(')');

            JSignature.InformalType ret = readInformal();
            List<JSignature.InformalType> exceptions = new ArrayList<>();
            while (hasNext() && peek() == '^') {
                skip('^');
                exceptions.add(readInformal());
            }
            return new JSignature.MethodDeclaration(declarations, parameters, ret, exceptions);
        } else {
            JSignature.InformalType superclass = readClassType();
            List<JSignature.InformalType> interfaces = new ArrayList<>();
            while (hasNext()) {
                interfaces.add(readClassType());
            }

            return new JSignature.ClassDeclaration(declarations, superclass, interfaces);
        }
    }

    private JSignature.MethodDeclaration readMethodDeclaration() {
        List<JSignature.VarTypeDeclaration> declarations = isParametersNext() ? readVarDeclarations() : Collections.emptyList();
        List<JSignature.InformalType> parameters = new ArrayList<>();
        skip('(');
        while (hasNext() && peek() != ')') {
            parameters.add(readInformal());
        }
        skip(')');

        JSignature.InformalType ret = readInformal();
        List<JSignature.InformalType> exceptions = new ArrayList<>();
        while (hasNext() && peek() == '^') {
            skip('^');
            exceptions.add(readInformal());
        }
        return new JSignature.MethodDeclaration(declarations, parameters, ret, exceptions);
    }

    private JSignature.ClassDeclaration readClassDeclaration() {
        List<JSignature.VarTypeDeclaration> declarations = isParametersNext() ? readVarDeclarations() : Collections.emptyList();
        JSignature.InformalType superclass = readClassType();
        List<JSignature.InformalType> interfaces = new ArrayList<>();
        while (hasNext()) {
            interfaces.add(readClassType());
        }

        return new JSignature.ClassDeclaration(declarations, superclass, interfaces);
    }

    private JSignature.InformalType readInformal() {
        if (isArrayNext()) {
            return readArray();
        } else if (isVarRefNext()) {
            return readVarRef();
        } else if (isWildNext()) {
            return readWild();
        } else {
            return readClassType();
        }
    }

    private JSignature.InformalType readClassType() {
        return readClassType(null);
    }

    private JSignature.InformalType readClassType(JSignature.Parameterized outer) {
        JSignature.Type descriptor = readDescriptor();
        if (!descriptor.isPrimitive()) {
            if (!isParametersNext() && (!hasNext() || peek() != '.') && outer == null) {
                return descriptor;
            } else {
                List<JSignature.InformalType> parameters;
                if (isParametersNext()) {
                    parameters = readParameters();
                } else {
                    parameters = new ArrayList<>();
                }

                JSignature.Parameterized parameterized = new JSignature.Parameterized(outer, descriptor, parameters);
                if (hasNext() && next() == '.') {
                    skip('.');
                    return readClassType(parameterized);
                } else {
                    return parameterized;
                }
            }
        } else if (outer == null) {
            return descriptor;
        } else {
            return fail("Inner type cannot be a primitive");
        }
    }

    private boolean isArrayNext() {
        if (!hasNext()) return false;
        return peek() == '[';
    }

    private JSignature.Array readArray() {
        if (!this.hasNext()) fail("Expected array start ( [ ), got EOF");
        if (peek() != '[') fail("Expected array start ( [ ), got " + Character.toString(peek()));

        skip('[');
        return new JSignature.Array(readInformal());
    }

    private boolean isDescriptorNext() {
        if (!hasNext()) return false;
        int c = peek();
        return c == 'L' || c == 'B' || c == 'C' || c == 'D' || c == 'F' || c == 'I' || c == 'J' || c == 'S' || c == 'Z';
    }

    private JSignature.Type readDescriptor() {
        if (!this.hasNext()) fail("Expected descriptor start (L, B, C, D, F, I, J, S, Z), got EOF");

        int typeInd = this.next();

        return switch (typeInd) {
            case 'L' -> {
                List<String> packageName = new ArrayList<>();
                String className = null;
                while (hasNext() && peek() != ';' && peek() != '<') {
                    String name = readUntil(c -> c == '/' || c == ';' || c == '<');
                    if (peek() == ';' || peek() == '<') {
                        className = name;
                    } else {
                        packageName.add(name);
                        skip('/');
                    }
                }
                skip(';');

                yield new JSignature.Type(new JDescriptor.Class(packageName.isEmpty() ?
                        JDescriptor.Class.DEFAULT_PACKAGE : packageName.toArray(String[]::new), className));
            }
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> new JSignature.Type(JDescriptor.Primitive.of(Character.toString(typeInd)));
            default -> fail("Expected descriptor start (L, B, C, D, F, I, J, S, Z), got " + Character.toString(typeInd));
        };
    }

    private boolean isVarRefNext() {
        if (!hasNext()) return false;
        return peek() == 'T';
    }

    private JSignature.VarType readVarRef() {
        if (!this.hasNext()) fail("Expected type variable reference start (T), got EOF");
        if (peek() != 'T') fail("Expected type variable reference start (T), got " + Character.toString(peek()));

        skip('T');
        StringBuilder sb = new StringBuilder();
        readUntil(c -> c == ';', sb);
        skip(';');

        return new JSignature.VarType(sb.toString());
    }

    private boolean isWildNext() {
        if (!hasNext()) return false;
        return peek() == '+' || peek() == '-' || peek() == '*';
    }

    private JSignature.WildType readWild() {
        if (!this.hasNext()) fail("Expected wild type start (+, -, *) start, got EOF");
        if (peek() != '-' || peek() != '+' || peek() != '*') fail("Expected wild type start (+, -, *), got " + Character.toString(peek()));

        int wildInd = next();
        if (wildInd == '-') {
            return new JSignature.WildType(null, readInformal());
        } else if (wildInd == '+') {
            return new JSignature.WildType(readInformal(), null);
        } else {
            return new JSignature.WildType(null, null);
        }
    }

    private boolean isParametersNext() {
        if (!hasNext()) return false;
        return peek() == '<';
    }

    private List<JSignature.InformalType> readParameters() {
        if (!this.hasNext()) fail("Expected type parameters start ( < ), got EOF");
        if (peek() != '<') fail("Expected type parameters start ( < ), got " + Character.toString(peek()));

        skip('<');

        List<JSignature.InformalType> result = new ArrayList<>();
        while (hasNext() && peek() != '>') {
            result.add(readInformal());
        }
        skip('>');
        return result;
    }

    private List<JSignature.VarTypeDeclaration> readVarDeclarations() {
        if (!this.hasNext()) fail("Expected type parameters declaration start ( < ), got EOF");
        if (peek() != '<') fail("Expected type parameters declaration start ( < ), got " + Character.toString(peek()));

        skip('<');

        List<JSignature.VarTypeDeclaration> result = new ArrayList<>();
        while (hasNext() && peek() != '>') {
            String name = readUntil(c -> c == ':');
            skip(':');
            JSignature.InformalType classBound;
            if (hasNext() && peek() == ':') {
                classBound = null;
            } else {
                classBound = readInformal();
            }

            List<JSignature.InformalType> interBounds = new ArrayList<>();
            while (hasNext() && peek() == ':') {
                skip(':');
                interBounds.add(readInformal());
            }

            result.add(new JSignature.VarTypeDeclaration(name, classBound, interBounds));
            skip(':');
        }
        skip('>');
        return result;
    }
}
