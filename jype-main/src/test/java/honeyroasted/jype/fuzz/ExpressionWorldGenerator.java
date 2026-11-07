package honeyroasted.jype.fuzz;

import honeyroasted.jype.system.expression.JExpressionInformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExpressionWorldGenerator {
    private static final String[] PRIMITIVES = {"boolean", "byte", "short", "char", "int", "long", "float", "double"};
    private static final String[] PLAIN = {"Object", "String", "Integer", "Number", "Long", "Character", "Double", "Boolean", "CharSequence", "java.io.Serializable", "Runnable", "RuntimeException", "Exception", "Comparable<String>"};
    private static final String[] GENERIC1 = {"Comparable", "java.util.List", "java.util.Collection", "java.util.ArrayList", "Iterable", "java.util.function.Supplier", "java.util.function.Predicate", "java.util.function.Consumer", "java.util.function.UnaryOperator", "java.util.concurrent.Callable"};
    private static final String[] GENERIC2 = {"java.util.Map", "java.util.function.Function", "java.util.function.BiConsumer"};
    private static final String[] METHOD_NAMES = {"m0", "m1", "m2", "m3"};
    private static final String[][] TYPE_PARAMS = {{}, {"A"}, {"A extends Number"}, {"A extends Comparable<A>"}, {"A", "B extends A"}, {"A", "B"}, {"A extends CharSequence"}};

    private static final String[][] FIXED_FIELDS = {
            {"String", "s0"}, {"Integer", "i0"}, {"int", "n0"}, {"long", "l0"}, {"double", "d0"}, {"char", "c0"}, {"Object", "o0"},
            {"java.util.List<String>", "ls"}, {"java.util.List<Integer>", "li"}, {"java.util.ArrayList<String>", "als"},
            {"java.util.Map<String, Integer>", "msi"}, {"Number", "num"}, {"CharSequence", "cs"}, {"java.util.List<? extends Number>", "lnum"},
            {"java.util.function.Function<String, Integer>", "fsi"}, {"Runnable", "rr"}, {"String[]", "sa"}, {"int[]", "ia"},
            {"java.util.List<? super Integer>", "lsup"}, {"java.util.List", "lraw"}, {"byte", "b0"}, {"short", "sh0"}
    };

    private static final String[][] LIBRARY = {
            {"id", "static <T> T id(T t)", "1"},
            {"listOf", "static <T> java.util.List<T> listOf(T... ts)", "v"},
            {"apply", "static <T, R> R apply(java.util.function.Function<T, R> f, T t)", "2"},
            {"max", "static <T extends Comparable<? super T>> T max(java.util.Collection<? extends T> c)", "1"},
            {"over", "static void over(Object o)", "1"},
            {"over", "static void over(String o)", "1"},
            {"over", "static void over(int o)", "1"},
            {"over", "static void over(Integer o)", "1"},
            {"over", "static void over(long o)", "1"},
            {"over", "static void over(Object... o)", "v"},
            {"run", "static void run(Runnable r)", "1"},
            {"run", "static <T> T run(java.util.concurrent.Callable<T> c)", "1"},
            {"consume", "static void consume(java.util.function.Consumer<String> c)", "1"},
            {"consume", "static int consume(java.util.function.Function<String, Integer> f)", "1"},
            {"pick", "static <T> T pick(T a, T b)", "2"},
            {"sink", "static <T> void sink(java.util.List<? super T> l, T t)", "2"},
            {"risky", "static <E extends Exception> void risky(F3<E> f) throws E", "1"},
            {"sup", "static <T> java.util.function.Supplier<T> sup(T t)", "1"},
            {"mapOf", "static <K, V> java.util.Map<K, V> mapOf(K k, V v)", "2"},
            {"str", "static String str(Object o)", "1"},
            {"arr", "static <T> T[] arr(T... ts)", "v"},
            {"sum", "static <T extends Number> double sum(java.util.List<T> l)", "1"},
            {"first", "static <T> T first(java.util.List<? extends T> l)", "1"},
            {"copy", "static <T> void copy(java.util.List<? super T> dst, java.util.List<? extends T> src)", "2"},
            {"compose", "static <A, B, C> java.util.function.Function<A, C> compose(java.util.function.Function<A, B> f, java.util.function.Function<B, C> g)", "2"},
            {"len", "static int len(CharSequence c)", "1"},
            {"len", "static int len(Object[] c)", "1"}
    };

    private static final String[][] INSTANCE_METHODS = {
            {"toString", "0"}, {"hashCode", "0"}, {"length", "0"}, {"size", "0"}, {"get", "1"}, {"isEmpty", "0"}, {"equals", "1"},
            {"compareTo", "1"}, {"apply", "1"}, {"add", "1"}, {"charAt", "1"}, {"intValue", "0"}, {"getClass", "0"}, {"clone", "0"},
            {"subList", "2"}, {"contains", "1"}, {"run", "0"}, {"keySet", "0"}
    };

    private final Random random;
    private final Map<String, String> holders = new LinkedHashMap<>();
    private final List<String> valueFields = new ArrayList<>();
    private final List<String> valueTypes = new ArrayList<>();
    private final List<String> methodNames = new ArrayList<>();
    private final List<String> methodArity = new ArrayList<>();
    private final List<List<String>> scopes = new ArrayList<>();
    private int lambdaCounter;
    private boolean instanceProbe;
    private static final String[][] LOCALS = {{"String", "l0"}, {"int", "l1"}, {"java.util.List<String>", "l2"}, {"Object", "l3"}, {"int[]", "l4"}, {"Integer", "l5"}};

    public ExpressionWorldGenerator(long seed) {
        this.random = new Random(seed);
    }

    public ExpressionWorld generate(String packageName, int probeCount) {
        List<String> decls = new ArrayList<>();
        StringBuilder header = new StringBuilder("package ").append(packageName).append(";\n");
        header.append("interface F0 { int f(String s); }\n");
        header.append("interface F1<X> { X f(X a, X b); }\n");
        header.append("interface F2 { void f() throws Exception; }\n");
        header.append("interface F3<E extends Exception> { void f() throws E; }\n");

        for (String[] f : FIXED_FIELDS) {
            this.valueFields.add(f[1]);
            this.valueTypes.add(f[0]);
            decls.add("    static " + f[0] + " " + f[1] + ";");
        }
        for (int i = 0; i < 4; i++) {
            String type = this.type(List.of(), 0, false, true);
            String name = "v" + i;
            this.valueFields.add(name);
            this.valueTypes.add(type);
            decls.add("    static " + type + " " + name + ";");
        }
        decls.add("    static boolean flag;");
        decls.add("    static void staticContext() { }");

        for (String[] lib : LIBRARY) {
            decls.add("    " + lib[1] + " { throw new RuntimeException(); }");
            this.methodNames.add(lib[0]);
            this.methodArity.add(lib[2]);
        }
        int methods = 2 + this.random.nextInt(4);
        for (int i = 0; i < methods; i++) {
            String[] tps = TYPE_PARAMS[this.random.nextInt(TYPE_PARAMS.length)];
            List<String> vars = new ArrayList<>();
            for (String tp : tps) {
                vars.add(tp.split(" ")[0]);
            }
            String name = METHOD_NAMES[this.random.nextInt(METHOD_NAMES.length)];
            int arity = this.random.nextInt(3);
            StringBuilder sb = new StringBuilder("    static ");
            if (tps.length > 0) {
                sb.append("<").append(String.join(", ", tps)).append("> ");
            }
            sb.append(this.random.nextInt(6) == 0 ? "void" : this.type(vars, 0, false, true)).append(" ").append(name).append("(");
            for (int p = 0; p < arity; p++) {
                if (p > 0) {
                    sb.append(", ");
                }
                sb.append(this.type(vars, 0, false, true)).append(" a").append(p);
            }
            sb.append(") { throw new RuntimeException(); }");
            decls.add(sb.toString());
            this.methodNames.add(name);
            this.methodArity.add(String.valueOf(arity));
        }

        List<Probe> probes = new ArrayList<>();
        for (int i = 0; i < probeCount; i++) {
            if (this.random.nextBoolean()) {
                String[] directed = this.directedTarget();
                probes.add(new Probe(i, this.holder(directed[0]), directed[0], this.directed(Integer.parseInt(directed[1])), false));
            } else {
                String target = this.target();
                this.instanceProbe = this.random.nextInt(4) == 0;
                probes.add(new Probe(i, this.holder(target), target, this.expr(0), this.instanceProbe));
            }
        }
        this.holders.forEach((type, field) -> decls.add("    static " + type + " " + field + ";"));
        return new ExpressionWorld(packageName, header.toString(), decls, probes);
    }

    private String target() {
        int k = this.random.nextInt(10);
        if (k < 4) {
            return this.valueTypes.get(this.random.nextInt(this.valueTypes.size()));
        } else if (k < 5) {
            return "Object";
        } else if (k < 7) {
            String[] fns = {"Runnable", "java.util.function.Supplier<String>", "java.util.function.Function<String, Integer>", "java.util.function.Consumer<String>", "F0", "F2",
                    "java.util.function.Function<Integer, String>", "java.util.function.Supplier<? extends Number>", "java.util.function.Predicate<? super String>",
                    "java.util.concurrent.Callable<Object>", "java.util.function.UnaryOperator<String>", "java.util.function.BiFunction<String, Integer, Object>"};
            return fns[this.random.nextInt(fns.length)];
        }
        return this.type(List.of(), 0, false, true);
    }

    private String[] directedTarget() {
        int f = this.random.nextInt(this.valueFields.size());
        String type = this.valueTypes.get(f);
        String[] widen = {type, type, "Object", type.startsWith("java.util.List") ? "java.util.Collection<?>" : type};
        return new String[]{widen[this.random.nextInt(widen.length)], String.valueOf(f)};
    }

    private FuzzExpr field(int index) {
        return new FuzzExpr.Field(this.valueFields.get(index));
    }

    private FuzzExpr directed(int f) {
        FuzzExpr base = this.field(f);
        int other = this.random.nextInt(this.valueFields.size());
        String p = "q" + (this.lambdaCounter++);
        return switch (this.random.nextInt(16)) {
            case 0 -> base;
            case 1 -> new FuzzExpr.StaticCall("id", List.of(), List.of(base));
            case 2 -> new FuzzExpr.StaticCall("pick", List.of(), List.of(base, this.random.nextBoolean() ? base : this.field(other)));
            case 3 -> new FuzzExpr.StaticCall("first", List.of(), List.of(new FuzzExpr.StaticCall("listOf", List.of(), List.of(base))));
            case 4 -> new FuzzExpr.StaticCall("apply", List.of(), List.of(new FuzzExpr.Lambda(List.of(p), null, new FuzzExpr.LambdaParam(p), 0), base));
            case 5 -> new FuzzExpr.StaticCall("run", List.of(), List.of(new FuzzExpr.Lambda(List.of(), null, base, this.random.nextInt(4) == 0 ? 1 : 0)));
            case 6 -> new FuzzExpr.InstanceCall(new FuzzExpr.StaticCall("sup", List.of(), List.of(base)), "get", List.of());
            case 7 -> new FuzzExpr.Conditional(new FuzzExpr.Field("flag"), base, this.random.nextBoolean() ? base : this.field(other));
            case 8 -> new FuzzExpr.StaticCall("id", List.of(this.typeRef(this.boxed(this.valueTypes.get(f)))), List.of(base));
            case 9 -> new FuzzExpr.StaticCall("apply", List.of(), List.of(new FuzzExpr.Lambda(List.of(p), null, new FuzzExpr.StaticCall("id", List.of(), List.of(new FuzzExpr.LambdaParam(p))), 0), base));
            case 10 -> new FuzzExpr.InstanceCall(new FuzzExpr.StaticCall("compose", List.of(), List.of(
                    new FuzzExpr.Lambda(List.of(p), null, new FuzzExpr.LambdaParam(p), 0),
                    new FuzzExpr.Lambda(List.of(p + "b"), null, new FuzzExpr.LambdaParam(p + "b"), 0))), "apply", List.of(base));
            case 11 -> new FuzzExpr.StaticCall("max", List.of(), List.of(new FuzzExpr.StaticCall("listOf", List.of(), List.of(base, base))));
            case 12 -> new FuzzExpr.Cast(this.typeRef(this.valueTypes.get(f)), new FuzzExpr.StaticCall("id", List.of(), List.of(this.field(other))));
            case 13 -> new FuzzExpr.InstanceCall(new FuzzExpr.StaticCall("mapOf", List.of(), List.of(new FuzzExpr.Literal("\"k\"", "k"), base)), "get", List.of(new FuzzExpr.Literal("\"k\"", "k")));
            case 14 -> new FuzzExpr.StaticCall("run", List.of(), List.of(new FuzzExpr.StaticRef("M", "str")));
            default -> new FuzzExpr.StaticCall("pick", List.of(), List.of(base, new FuzzExpr.Literal("null", null)));
        };
    }

    private String boxed(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "char" -> "Character";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "boolean" -> "Boolean";
            case "float" -> "Float";
            default -> type;
        };
    }

    private String holder(String type) {
        return this.holders.computeIfAbsent(type, t -> "t" + this.holders.size());
    }

    private FuzzExpr.TypeRef typeRef(String source) {
        return new FuzzExpr.TypeRef(source, this.holder(source));
    }

    private FuzzExpr leaf() {
        List<String> inScope = new ArrayList<>();
        this.scopes.forEach(inScope::addAll);
        if (!inScope.isEmpty() && this.random.nextInt(2) == 0) {
            return new FuzzExpr.LambdaParam(inScope.get(this.random.nextInt(inScope.size())));
        }
        if (this.random.nextInt(3) == 0) {
            return this.literal();
        }
        return new FuzzExpr.Field(this.valueFields.get(this.random.nextInt(this.valueFields.size())));
    }

    private FuzzExpr local() {
        String[] l = LOCALS[this.random.nextInt(LOCALS.length)];
        return new FuzzExpr.Local(l[1], this.typeRef(l[0]));
    }

    private FuzzExpr variable() {
        return switch (this.random.nextInt(4)) {
            case 0 -> this.local();
            case 1 -> new FuzzExpr.Index(new FuzzExpr.Field(this.random.nextBoolean() ? "sa" : "ia"), new FuzzExpr.Literal("0", 0));
            default -> new FuzzExpr.Field(this.valueFields.get(this.random.nextInt(this.valueFields.size())));
        };
    }

    private FuzzExpr extra(int depth) {
        int k = this.random.nextInt(9);
        return switch (k) {
            case 0 -> this.local();
            case 1 -> new FuzzExpr.ClassLit(this.typeRef(this.random.nextBoolean() ? PLAIN[this.random.nextInt(PLAIN.length)].replace("<String>", "") : PRIMITIVES[this.random.nextInt(PRIMITIVES.length)] + (this.random.nextBoolean() ? "[]" : "")));
            case 2 -> this.instanceProbe ? new FuzzExpr.This() : new FuzzExpr.ClassLit(this.typeRef("java.util.List"));
            case 3 -> {
                String[] ops = {null, "+", "-", "*", "&", "<<"};
                JExpressionInformation.BinaryOperator.Kind[] kinds = {null, JExpressionInformation.BinaryOperator.Kind.ADD, JExpressionInformation.BinaryOperator.Kind.SUB, JExpressionInformation.BinaryOperator.Kind.MUL, JExpressionInformation.BinaryOperator.Kind.AND, JExpressionInformation.BinaryOperator.Kind.SHL};
                int o = this.random.nextInt(ops.length);
                yield new FuzzExpr.Assign(this.variable(), kinds[o], ops[o], this.expr(depth + 1));
            }
            case 4 -> new FuzzExpr.IncDec(this.variable(), this.random.nextBoolean(), this.random.nextBoolean());
            case 5 -> new FuzzExpr.Index(this.random.nextBoolean() ? new FuzzExpr.Field(this.random.nextBoolean() ? "sa" : "ia") : this.local(), this.expr(depth + 1));
            case 6 -> {
                List<FuzzExpr> results = new ArrayList<>();
                int n = 1 + this.random.nextInt(3);
                for (int i = 0; i < n; i++) {
                    results.add(this.expr(depth + 1));
                }
                yield new FuzzExpr.SwitchExpr(this.random.nextBoolean() ? this.leaf() : this.local(), results, this.random.nextInt(3) == 0);
            }
            case 7 -> {
                String[][] anons = {{"Object", "", "0"}, {"java.util.ArrayList", "", "1"}, {"Runnable", "public void run() { }", "0"}, {"Comparable", "public int compareTo(String o) { return 0; }", "2"}, {"java.util.ArrayList", "", "3"}};
                String[] a = anons[this.random.nextInt(anons.length)];
                List<FuzzExpr.TypeRef> targs = a[2].equals("1") || a[2].equals("2") ? List.of(this.typeRef("String")) : List.of();
                List<FuzzExpr> args = a[2].equals("3") ? List.of(this.expr(depth + 1)) : List.of();
                yield new FuzzExpr.Anon(a[0], targs, a[2].equals("3") || (a[2].equals("1") && this.random.nextBoolean()), args, a[1]);
            }
            default -> {
                int idx = this.random.nextInt(this.methodNames.size());
                String ar = this.methodArity.get(idx);
                int arity = ar.equals("v") ? this.random.nextInt(3) : Integer.parseInt(ar);
                List<FuzzExpr> args = new ArrayList<>();
                for (int i = 0; i < arity; i++) {
                    args.add(this.expr(depth + 1));
                }
                yield new FuzzExpr.Unqualified(this.methodNames.get(idx), args);
            }
        };
    }

    private FuzzExpr expr(int depth) {
        if (depth <= 2 && this.random.nextInt(6) == 0) {
            return this.extra(depth);
        }
        int k = depth > 2 ? this.random.nextInt(40) : this.random.nextInt(100);
        if (k < 35) {
            return this.leaf();
        } else if (k < 60) {
            int idx = this.random.nextInt(this.methodNames.size());
            String ar = this.methodArity.get(idx);
            int arity = ar.equals("v") ? this.random.nextInt(4) : Integer.parseInt(ar);
            if (this.random.nextInt(10) == 0) {
                arity = Math.max(0, arity + this.random.nextInt(3) - 1);
            }
            List<FuzzExpr> args = new ArrayList<>();
            for (int i = 0; i < arity; i++) {
                args.add(this.expr(depth + 1));
            }
            List<FuzzExpr.TypeRef> targs = new ArrayList<>();
            if (this.random.nextInt(12) == 0) {
                targs.add(this.typeRef(this.type(List.of(), 1, false, false)));
            }
            return new FuzzExpr.StaticCall(this.methodNames.get(idx), targs, args);
        } else if (k < 68) {
            String[] im = INSTANCE_METHODS[this.random.nextInt(INSTANCE_METHODS.length)];
            List<FuzzExpr> args = new ArrayList<>();
            for (int i = 0; i < Integer.parseInt(im[1]); i++) {
                args.add(this.random.nextBoolean() ? new FuzzExpr.Literal("0", 0) : this.expr(depth + 1));
            }
            return new FuzzExpr.InstanceCall(this.leaf(), im[0], args);
        } else if (k < 80) {
            int count = this.random.nextInt(3);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                names.add("p" + (this.lambdaCounter++));
            }
            List<FuzzExpr.TypeRef> explicit = null;
            if (count > 0 && this.random.nextInt(3) == 0) {
                explicit = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    explicit.add(this.typeRef(this.random.nextBoolean() ? "String" : this.type(List.of(), 1, false, true)));
                }
            }
            this.scopes.add(names);
            FuzzExpr body = this.expr(depth + 1);
            this.scopes.removeLast();
            int form = this.random.nextInt(5) == 0 ? (this.random.nextBoolean() ? 1 : 2) : 0;
            if (form == 2 && !(body instanceof FuzzExpr.StaticCall) && !(body instanceof FuzzExpr.InstanceCall) && !(body instanceof FuzzExpr.New)) {
                form = 1;
            }
            return new FuzzExpr.Lambda(names, explicit, body, form);
        } else if (k < 86) {
            int r = this.random.nextInt(10);
            return switch (r) {
                case 0 -> new FuzzExpr.StaticRef("String", "valueOf");
                case 1 -> new FuzzExpr.StaticRef("Integer", "parseInt");
                case 2 -> new FuzzExpr.StaticRef("Object", "toString");
                case 3 -> new FuzzExpr.StaticRef("String", "length");
                case 4 -> new FuzzExpr.CtorRef(this.typeRef("java.util.ArrayList"));
                case 5 -> new FuzzExpr.CtorRef(this.typeRef(this.random.nextBoolean() ? "Object[]" : "String[]"));
                case 6 -> new FuzzExpr.StaticRef("Integer", "compare");
                case 7 -> new FuzzExpr.StaticRef("String", "concat");
                default -> new FuzzExpr.StaticRef("M", this.methodNames.get(this.random.nextInt(this.methodNames.size())));
            };
        } else if (k < 90) {
            int r = this.random.nextInt(4);
            return switch (r) {
                case 0 -> new FuzzExpr.New("java.util.ArrayList", List.of(), true, List.of());
                case 1 -> new FuzzExpr.New("java.util.ArrayList", List.of(this.typeRef(this.type(List.of(), 1, false, false))), false, List.of());
                case 2 -> new FuzzExpr.New("StringBuilder", List.of(), false, this.random.nextBoolean() ? List.of() : List.of(this.expr(depth + 1)));
                default -> new FuzzExpr.New("java.util.ArrayList", List.of(), true, List.of(this.expr(depth + 1)));
            };
        } else if (k < 94) {
            return new FuzzExpr.Cast(this.typeRef(this.random.nextBoolean() ? this.valueTypes.get(this.random.nextInt(this.valueTypes.size())) : this.type(List.of(), 0, false, true)), this.expr(depth + 1));
        } else if (k < 97) {
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
        return switch (this.random.nextInt(10)) {
            case 0 -> new FuzzExpr.Literal("1", 1);
            case 1 -> new FuzzExpr.Literal("300", 300);
            case 2 -> new FuzzExpr.Literal("5L", 5L);
            case 3 -> new FuzzExpr.Literal("2.5", 2.5);
            case 4 -> new FuzzExpr.Literal("'c'", 'c');
            case 5 -> new FuzzExpr.Literal("true", true);
            case 6, 9 -> new FuzzExpr.Literal("\"s\"", "s");
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
