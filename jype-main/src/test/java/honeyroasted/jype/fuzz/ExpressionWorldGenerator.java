package honeyroasted.jype.fuzz;

import honeyroasted.jype.system.expression.JExpressionInformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExpressionWorldGenerator {
    private static final String[] PRIMITIVES = {"boolean", "byte", "short", "char", "int", "long", "float", "double"};
    private static final String[] PLAIN = {"Object", "String", "Integer", "Number", "Long", "Character", "Double", "Boolean", "CharSequence", "java.io.Serializable", "Runnable", "RuntimeException", "Exception"};
    private static final String[] GENERIC1 = {"Comparable", "java.util.List", "java.util.Collection", "java.util.ArrayList", "Iterable", "java.util.function.Supplier", "java.util.function.Predicate", "java.util.function.Consumer", "java.util.function.UnaryOperator", "java.util.concurrent.Callable"};
    private static final String[] GENERIC2 = {"java.util.Map", "java.util.function.Function", "java.util.function.BiConsumer"};
    private static final String[] METHOD_NAMES = {"m0", "m1", "m2", "m3"};
    private static final String[][] TYPE_PARAMS = {{}, {"A"}, {"A extends Number"}, {"A extends Comparable<A>"}, {"A", "B extends A"}, {"A", "B"}, {"A extends CharSequence"}};

    private final Random random;
    private final Map<String, String> holders = new LinkedHashMap<>();
    private final List<String> valueFields = new ArrayList<>();
    private final List<String> valueTypes = new ArrayList<>();
    private final List<String> booleanFields = new ArrayList<>();
    private final List<String> methodNames = new ArrayList<>();
    private final List<Integer> methodArities = new ArrayList<>();

    public ExpressionWorldGenerator(long seed) {
        this.random = new Random(seed);
    }

    public ExpressionWorld generate(String packageName, int probeCount) {
        List<String> decls = new ArrayList<>();
        StringBuilder header = new StringBuilder("package ").append(packageName).append(";\n");
        header.append("interface F0 { int f(String s); }\n");
        header.append("interface F1<X> { X f(X a, X b); }\n");
        header.append("interface F2 { void f() throws Exception; }\n");

        for (int i = 0; i < 10; i++) {
            String type = this.type(List.of(), 0, false, true);
            String name = "v" + i;
            this.valueFields.add(name);
            this.valueTypes.add(type);
            decls.add("    static " + type + " " + name + ";");
        }
        decls.add("    static boolean flag;");
        this.booleanFields.add("flag");

        int methods = 3 + this.random.nextInt(6);
        for (int i = 0; i < methods; i++) {
            String[] tps = TYPE_PARAMS[this.random.nextInt(TYPE_PARAMS.length)];
            List<String> vars = new ArrayList<>();
            for (String tp : tps) {
                vars.add(tp.split(" ")[0]);
            }
            String name = METHOD_NAMES[this.random.nextInt(METHOD_NAMES.length)];
            int arity = this.random.nextInt(3);
            boolean varargs = arity > 0 && this.random.nextInt(6) == 0;
            StringBuilder sb = new StringBuilder("    static ");
            if (tps.length > 0) {
                sb.append("<").append(String.join(", ", tps)).append("> ");
            }
            sb.append(this.random.nextInt(6) == 0 ? "void" : this.type(vars, 0, false, true)).append(" ").append(name).append("(");
            for (int p = 0; p < arity; p++) {
                if (p > 0) {
                    sb.append(", ");
                }
                String pt = this.type(vars, 0, false, true);
                if (varargs && p == arity - 1) {
                    pt = pt + "...";
                }
                sb.append(pt).append(" a").append(p);
            }
            sb.append(") { throw new RuntimeException(); }");
            decls.add(sb.toString());
            this.methodNames.add(name);
            this.methodArities.add(arity);
        }

        List<Probe> probes = new ArrayList<>();
        for (int i = 0; i < probeCount; i++) {
            String target = this.type(List.of(), 0, false, true);
            String holder = this.holder(target);
            probes.add(new Probe(i, holder, target, this.expr(0)));
        }
        this.holders.forEach((type, field) -> decls.add("    static " + type + " " + field + ";"));
        return new ExpressionWorld(packageName, header.toString(), decls, probes);
    }

    private String holder(String type) {
        return this.holders.computeIfAbsent(type, t -> "t" + this.holders.size());
    }

    private FuzzExpr.TypeRef typeRef(String source) {
        return new FuzzExpr.TypeRef(source, this.holder(source));
    }

    private FuzzExpr expr(int depth) {
        int k = depth > 2 ? this.random.nextInt(35) : this.random.nextInt(100);
        if (k < 20) {
            return new FuzzExpr.Field(this.valueFields.get(this.random.nextInt(this.valueFields.size())));
        } else if (k < 35) {
            return this.literal();
        } else if (k < 60) {
            int idx = this.random.nextInt(this.methodNames.size());
            int arity = this.methodArities.get(idx);
            if (this.random.nextInt(8) == 0) {
                arity = Math.max(0, arity + this.random.nextInt(3) - 1);
            }
            List<FuzzExpr> args = new ArrayList<>();
            for (int i = 0; i < arity; i++) {
                args.add(this.expr(depth + 1));
            }
            List<FuzzExpr.TypeRef> targs = new ArrayList<>();
            if (this.random.nextInt(10) == 0) {
                targs.add(this.typeRef(this.type(List.of(), 1, false, false)));
            }
            return new FuzzExpr.StaticCall(this.methodNames.get(idx), targs, args);
        } else if (k < 70) {
            int count = this.random.nextInt(3);
            List<FuzzExpr.TypeRef> explicit = null;
            if (count > 0 && this.random.nextInt(3) == 0) {
                explicit = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    explicit.add(this.typeRef(this.type(List.of(), 1, false, true)));
                }
            }
            return new FuzzExpr.Lambda(explicit, count, this.expr(depth + 1));
        } else if (k < 78) {
            int r = this.random.nextInt(8);
            return switch (r) {
                case 0 -> new FuzzExpr.StaticRef("String", "valueOf");
                case 1 -> new FuzzExpr.StaticRef("Integer", "parseInt");
                case 2 -> new FuzzExpr.StaticRef("Object", "toString");
                case 3 -> new FuzzExpr.StaticRef("String", "length");
                case 4 -> new FuzzExpr.CtorRef(this.typeRef("java.util.ArrayList"));
                case 5 -> new FuzzExpr.CtorRef(this.typeRef(this.random.nextBoolean() ? "Object[]" : "String[]"));
                default -> new FuzzExpr.StaticRef("M", this.methodNames.get(this.random.nextInt(this.methodNames.size())));
            };
        } else if (k < 84) {
            int r = this.random.nextInt(4);
            return switch (r) {
                case 0 -> new FuzzExpr.New("java.util.ArrayList", List.of(), true, List.of());
                case 1 -> new FuzzExpr.New("java.util.ArrayList", List.of(this.typeRef(this.type(List.of(), 1, false, false))), false, List.of());
                case 2 -> new FuzzExpr.New("StringBuilder", List.of(), false, List.of());
                default -> new FuzzExpr.New("java.util.ArrayList", List.of(), true, List.of(this.expr(depth + 1)));
            };
        } else if (k < 90) {
            return new FuzzExpr.Cast(this.typeRef(this.type(List.of(), 0, false, true)), this.expr(depth + 1));
        } else if (k < 96) {
            FuzzExpr cond = this.random.nextInt(5) == 0 ? this.expr(depth + 1) : new FuzzExpr.Field("flag");
            return new FuzzExpr.Conditional(cond, this.expr(depth + 1), this.expr(depth + 1));
        } else if (k < 99) {
            JExpressionInformation.BinaryOperator.Kind[] kinds = {JExpressionInformation.BinaryOperator.Kind.ADD, JExpressionInformation.BinaryOperator.Kind.MUL, JExpressionInformation.BinaryOperator.Kind.LT, JExpressionInformation.BinaryOperator.Kind.EQ, JExpressionInformation.BinaryOperator.Kind.COND_AND, JExpressionInformation.BinaryOperator.Kind.AND, JExpressionInformation.BinaryOperator.Kind.SHL};
            String[] symbols = {"+", "*", "<", "==", "&&", "&", "<<"};
            int r = this.random.nextInt(kinds.length);
            return new FuzzExpr.Binary(kinds[r], symbols[r], this.expr(depth + 1), this.expr(depth + 1));
        }
        JExpressionInformation.UnaryOperator.Kind[] kinds = {JExpressionInformation.UnaryOperator.Kind.MINUS, JExpressionInformation.UnaryOperator.Kind.LOGICAL_COMPLEMENT, JExpressionInformation.UnaryOperator.Kind.BITWISE_COMPLEMENT};
        String[] symbols = {"-", "!", "~"};
        int r = this.random.nextInt(kinds.length);
        return new FuzzExpr.Unary(kinds[r], symbols[r], this.expr(depth + 1));
    }

    private FuzzExpr literal() {
        return switch (this.random.nextInt(9)) {
            case 0 -> new FuzzExpr.Literal("1", 1);
            case 1 -> new FuzzExpr.Literal("300", 300);
            case 2 -> new FuzzExpr.Literal("5L", 5L);
            case 3 -> new FuzzExpr.Literal("2.5", 2.5);
            case 4 -> new FuzzExpr.Literal("'c'", 'c');
            case 5 -> new FuzzExpr.Literal("true", true);
            case 6 -> new FuzzExpr.Literal("\"s\"", "s");
            case 7 -> new FuzzExpr.Literal("1.5f", 1.5f);
            default -> new FuzzExpr.Literal("null", null);
        };
    }

    private String type(List<String> vars, int depth, boolean allowWildcard, boolean allowPrimitive) {
        if (allowWildcard && this.random.nextInt(3) == 0) {
            int w = this.random.nextInt(3);
            if (w == 0 || depth > 2) {
                return "?";
            }
            return (w == 1 ? "? extends " : "? super ") + this.type(vars, depth + 1, false, false);
        }
        int k = depth > 2 ? this.random.nextInt(4) : this.random.nextInt(12);
        if (allowPrimitive && k == 0) {
            return PRIMITIVES[this.random.nextInt(PRIMITIVES.length)];
        } else if (k <= 2 && !vars.isEmpty()) {
            return vars.get(this.random.nextInt(vars.size()));
        } else if (k <= 4) {
            return PLAIN[this.random.nextInt(PLAIN.length)];
        } else if (k <= 7) {
            return GENERIC1[this.random.nextInt(GENERIC1.length)] + "<" + this.type(vars, depth + 1, true, false) + ">";
        } else if (k <= 8) {
            return GENERIC2[this.random.nextInt(GENERIC2.length)] + "<" + this.type(vars, depth + 1, true, false) + ", " + this.type(vars, depth + 1, true, false) + ">";
        } else if (k == 9) {
            return (this.random.nextBoolean() && allowPrimitive ? PRIMITIVES[this.random.nextInt(PRIMITIVES.length)] : this.type(vars, depth + 1, false, false)) + "[]";
        } else if (k == 10) {
            return this.random.nextBoolean() ? "F0" : "F2";
        }
        return "F1<" + this.type(vars, depth + 1, false, false) + ">";
    }
}
