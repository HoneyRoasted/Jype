package honeyroasted.jype.fuzz;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TypeWorldGenerator {
    private static final String[] PRIMITIVES = {"boolean", "byte", "short", "char", "int", "long", "float", "double"};
    private static final String[] PLAIN = {"Object", "String", "Integer", "Number", "Long", "Character", "Byte", "Short", "Double", "Boolean", "CharSequence", "java.io.Serializable", "Cloneable", "Runnable", "RuntimeException", "Exception"};
    private static final String[] GENERIC1 = {"Comparable", "java.util.List", "java.util.Collection", "java.util.ArrayList", "Iterable", "java.util.function.Supplier", "Class"};
    private static final String[] GENERIC2 = {"java.util.Map", "java.util.function.Function", "java.util.HashMap"};
    private static final List<String> HOLDER_VARS = List.of("T", "U", "V", "W", "S");
    private static final String HOLDER_PARAMS = "<T extends Number, U extends T, V extends Comparable<V>, W, S extends CharSequence & Comparable<String>>";

    private final Random random;
    private final List<GeneratedClass> classes = new ArrayList<>();

    public TypeWorldGenerator(long seed) {
        this.random = new Random(seed);
    }

    public TypeWorld generate(String packageName, int fieldCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n");
        int classCount = 2 + this.random.nextInt(5);
        for (int i = 0; i < classCount; i++) {
            sb.append(this.generateClass(i)).append("\n");
        }

        List<String> names = new ArrayList<>();
        List<String> types = new ArrayList<>();
        sb.append("abstract class H").append(HOLDER_PARAMS).append(" {\n");
        for (int i = 0; i < fieldCount; i++) {
            String type = this.fieldType(HOLDER_VARS, 0);
            names.add("f" + i);
            types.add(type);
            sb.append("    ").append(type).append(" f").append(i).append(";\n");
        }
        sb.append("}\n");
        return new TypeWorld(packageName, sb.toString(), names, types);
    }

    private String generateClass(int index) {
        boolean isInterface = this.random.nextInt(3) == 0;
        boolean isFinal = !isInterface && this.random.nextInt(4) == 0;
        int arity = this.random.nextInt(3);
        List<String> params = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            params.add("P" + index + "_" + i);
        }
        String name = "C" + index;
        StringBuilder sb = new StringBuilder();
        if (isFinal) {
            sb.append("final ");
        } else if (!isInterface) {
            sb.append("abstract ");
        }
        sb.append(isInterface ? "interface " : "class ").append(name);
        if (arity > 0) {
            sb.append("<");
            for (int i = 0; i < arity; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params.get(i));
                int b = this.random.nextInt(6);
                if (b == 0) {
                    sb.append(" extends Number");
                } else if (b == 1 && i > 0) {
                    sb.append(" extends ").append(params.getFirst());
                } else if (b == 2) {
                    sb.append(" extends Comparable<").append(params.get(i)).append(">");
                }
            }
            sb.append(">");
        }

        List<GeneratedClass> supers = this.classes.stream().filter(c -> !c.isFinal()).toList();
        if (!isInterface) {
            List<GeneratedClass> classSupers = supers.stream().filter(c -> !c.isInterface()).toList();
            if (!classSupers.isEmpty() && this.random.nextInt(3) != 0) {
                GeneratedClass sup = classSupers.get(this.random.nextInt(classSupers.size()));
                sb.append(" extends ").append(this.supertypeRef(sup, params));
            }
        }
        List<GeneratedClass> ifaceSupers = supers.stream().filter(GeneratedClass::isInterface).toList();
        if (!ifaceSupers.isEmpty() && this.random.nextInt(2) == 0) {
            GeneratedClass sup = ifaceSupers.get(this.random.nextInt(ifaceSupers.size()));
            sb.append(isInterface ? " extends " : " implements ").append(this.supertypeRef(sup, params));
        } else if (!isFinal && this.random.nextInt(4) == 0) {
            sb.append(isInterface ? " extends " : " implements ").append("Comparable<").append(params.isEmpty() ? name : params.getFirst()).append(">");
        }
        sb.append(" { }");
        this.classes.add(new GeneratedClass(name, isInterface, isFinal, params));
        return sb.toString();
    }

    private String supertypeRef(GeneratedClass sup, List<String> ownParams) {
        if (sup.arity() == 0 || this.random.nextInt(8) == 0) {
            return sup.name();
        }
        StringBuilder sb = new StringBuilder(sup.name()).append("<");
        for (int i = 0; i < sup.arity(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (!ownParams.isEmpty() && this.random.nextBoolean()) {
                sb.append(ownParams.get(this.random.nextInt(ownParams.size())));
            } else {
                sb.append(this.referenceType(List.of(), 2, false));
            }
        }
        return sb.append(">").toString();
    }

    private String fieldType(List<String> vars, int depth) {
        int k = this.random.nextInt(10);
        if (k == 0) {
            return PRIMITIVES[this.random.nextInt(PRIMITIVES.length)];
        } else if (k == 1) {
            String component = this.random.nextBoolean() ? PRIMITIVES[this.random.nextInt(PRIMITIVES.length)] : this.referenceType(vars, depth + 1, false);
            return component + (this.random.nextInt(4) == 0 ? "[][]" : "[]");
        }
        return this.referenceType(vars, depth, false);
    }

    private String referenceType(List<String> vars, int depth, boolean allowWildcard) {
        if (allowWildcard && this.random.nextInt(3) == 0) {
            int w = this.random.nextInt(3);
            if (w == 0 || depth > 2) {
                return "?";
            }
            return (w == 1 ? "? extends " : "? super ") + this.referenceType(vars, depth + 1, false);
        }
        int k = depth > 2 ? this.random.nextInt(4) : this.random.nextInt(12);
        if (k <= 2 && !vars.isEmpty() && this.random.nextBoolean()) {
            return vars.get(this.random.nextInt(vars.size()));
        } else if (k <= 3) {
            return PLAIN[this.random.nextInt(PLAIN.length)];
        } else if (k <= 5) {
            return GENERIC1[this.random.nextInt(GENERIC1.length)] + "<" + this.referenceType(vars, depth + 1, true) + ">";
        } else if (k == 6) {
            return GENERIC2[this.random.nextInt(GENERIC2.length)] + "<" + this.referenceType(vars, depth + 1, true) + ", " + this.referenceType(vars, depth + 1, true) + ">";
        } else if (k == 7) {
            return this.referenceType(vars, depth + 1, false) + "[]";
        }
        GeneratedClass c = this.classes.get(this.random.nextInt(this.classes.size()));
        if (c.arity() == 0 || this.random.nextInt(10) == 0) {
            return c.name();
        }
        StringBuilder sb = new StringBuilder(c.name()).append("<");
        for (int i = 0; i < c.arity(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.referenceType(vars, depth + 1, true));
        }
        return sb.append(">").toString();
    }
}
