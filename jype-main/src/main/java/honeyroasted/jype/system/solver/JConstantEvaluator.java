package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.Optional;

public final class JConstantEvaluator {
    private JConstantEvaluator() {
    }

    public record Value(Object value, JType type) {
    }

    public static Optional<Value> evaluate(JExpressionInformation expr, JTypeSystem system) {
        try {
            return Optional.ofNullable(eval(expr, system));
        } catch (ArithmeticException | ClassCastException e) {
            return Optional.empty();
        }
    }

    private static Value eval(JExpressionInformation expr, JTypeSystem system) {
        JTypeConstants c = system.constants();
        if (expr instanceof JExpressionInformation.Constant constant) {
            Object v = constant.value();
            if (v == null) {
                return null;
            }
            JType t = constant.type(system);
            // JLS §15.29 literals of primitive type and String literals
            return t instanceof JPrimitiveType || v instanceof String ? new Value(v, t) : null;
        } else if (expr instanceof JExpressionInformation.GetField<?> gf) {
            if (!(gf.source() instanceof JClassReference ref)) {
                return null;
            }
            Optional<JFieldReference> f = JFieldLookup.member(ref, gf.name());
            if (f.isEmpty() || !f.get().hasModifier(AccessFlag.FINAL) || f.get().constantValue().isEmpty()) {
                return null;
            }
            // JLS §15.29 simple or qualified names of constant variables
            return new Value(convert(f.get().constantValue().get(), f.get().type(), c), f.get().type());
        } else if (expr instanceof JExpressionInformation.Cast cast) {
            Value v = eval(cast.operand(), system);
            if (v == null) {
                return null;
            }
            JType target = cast.castType();
            if (target instanceof JPrimitiveType && v.type() instanceof JPrimitiveType) {
                // JLS §15.29 casts to primitive types
                return new Value(convert(v.value(), target, c), target);
            } else if (isString(target, system) && isString(v.type(), system)) {
                // JLS §15.29 casts to String
                return v;
            }
            return null;
        } else if (expr instanceof JExpressionInformation.UnaryOperator u) {
            Value v = eval(u.operand(), system);
            if (v == null) {
                return null;
            }
            Optional<JType> rt = JExpressionInformation.UnaryOperator.resultType(u.kind(), v.type(), system);
            if (rt.isEmpty()) {
                return null;
            }
            Object x = convert(v.value(), rt.get(), c);
            return switch (u.kind()) {
                case PLUS -> new Value(x, rt.get());
                case MINUS -> new Value(negate(x), rt.get());
                case BITWISE_COMPLEMENT -> new Value(x instanceof Long l ? ~l : ~((Integer) x), rt.get());
                case LOGICAL_COMPLEMENT -> new Value(!(Boolean) x, rt.get());
            };
        } else if (expr instanceof JExpressionInformation.BinaryOperator b) {
            Value l = eval(b.left(), system);
            Value r = eval(b.right(), system);
            if (l == null || r == null) {
                return null;
            }
            Optional<JType> rt = JExpressionInformation.BinaryOperator.resultType(b.kind(), l.type(), r.type(), system);
            if (rt.isEmpty()) {
                return null;
            }
            // JLS §15.29 binary operators over constant expressions
            return new Value(binary(b.kind(), l, r, rt.get(), system), rt.get());
        } else if (expr instanceof JExpressionInformation.Conditional cond) {
            Value test = eval(cond.condition(), system);
            Value a = eval(cond.whenTrue(), system);
            Value b = eval(cond.whenFalse(), system);
            if (test == null || a == null || b == null || !(test.value() instanceof Boolean flag)) {
                return null;
            }
            Optional<JType> rt = JExpressionInformation.Conditional.resultType(cond.whenTrue(), a.type(), cond.whenFalse(), b.type(), system);
            if (rt.isEmpty() || !(rt.get() instanceof JPrimitiveType || isString(rt.get(), system))) {
                return null;
            }
            // JLS §15.29 conditional operator over constant expressions
            Value chosen = flag ? a : b;
            return new Value(rt.get() instanceof JPrimitiveType ? convert(chosen.value(), rt.get(), c) : chosen.value(), rt.get());
        }
        return null;
    }

    private static boolean isString(JType t, JTypeSystem system) {
        return t instanceof JClassType ct && ct.classReference().typeEquals(system.tryResolve(String.class));
    }

    private static Object negate(Object x) {
        return switch (x) {
            case Integer i -> -i;
            case Long l -> -l;
            case Float f -> -f;
            case Double d -> -d;
            default -> throw new ClassCastException();
        };
    }

    private static Object binary(JExpressionInformation.BinaryOperator.Kind kind, Value l, Value r, JType rt, JTypeSystem system) {
        JTypeConstants c = system.constants();
        if (isString(rt, system)) {
            return String.valueOf(display(l)) + display(r);
        }
        if (kind == JExpressionInformation.BinaryOperator.Kind.COND_AND) {
            return (Boolean) l.value() && (Boolean) r.value();
        } else if (kind == JExpressionInformation.BinaryOperator.Kind.COND_OR) {
            return (Boolean) l.value() || (Boolean) r.value();
        }
        if (rt.typeEquals(c.booleanType()) && kind != JExpressionInformation.BinaryOperator.Kind.LT && kind != JExpressionInformation.BinaryOperator.Kind.GT
                && kind != JExpressionInformation.BinaryOperator.Kind.LE && kind != JExpressionInformation.BinaryOperator.Kind.GE
                && kind != JExpressionInformation.BinaryOperator.Kind.EQ && kind != JExpressionInformation.BinaryOperator.Kind.NE) {
            boolean a = (Boolean) l.value();
            boolean b = (Boolean) r.value();
            return switch (kind) {
                case AND -> a & b;
                case OR -> a | b;
                case XOR -> a ^ b;
                default -> throw new ClassCastException();
            };
        }
        switch (kind) {
            case SHL, SHR, USHR -> {
                long shift = ((Number) convert(r.value(), c.longType(), c)).longValue();
                if (rt.typeEquals(c.longType())) {
                    long a = (Long) convert(l.value(), c.longType(), c);
                    return switch (kind) {
                        case SHL -> a << shift;
                        case SHR -> a >> shift;
                        default -> a >>> shift;
                    };
                }
                int a = (Integer) convert(l.value(), c.intType(), c);
                return switch (kind) {
                    case SHL -> a << shift;
                    case SHR -> a >> shift;
                    default -> a >>> shift;
                };
            }
            case LT, GT, LE, GE, EQ, NE -> {
                JType promoted = JOperatorTypes.binaryNumericPromotion(l.type(), r.type(), system).map(t -> (JType) t).orElse(null);
                if (promoted == null) {
                    if (kind == JExpressionInformation.BinaryOperator.Kind.EQ) {
                        return l.value().equals(r.value());
                    } else if (kind == JExpressionInformation.BinaryOperator.Kind.NE) {
                        return !l.value().equals(r.value());
                    }
                    throw new ClassCastException();
                }
                double a = ((Number) convert(l.value(), c.doubleType(), c)).doubleValue();
                double b = ((Number) convert(r.value(), c.doubleType(), c)).doubleValue();
                if (promoted.typeEquals(c.longType()) || promoted.typeEquals(c.intType())) {
                    long la = ((Number) convert(l.value(), c.longType(), c)).longValue();
                    long lb = ((Number) convert(r.value(), c.longType(), c)).longValue();
                    return switch (kind) {
                        case LT -> la < lb;
                        case GT -> la > lb;
                        case LE -> la <= lb;
                        case GE -> la >= lb;
                        case EQ -> la == lb;
                        default -> la != lb;
                    };
                }
                return switch (kind) {
                    case LT -> a < b;
                    case GT -> a > b;
                    case LE -> a <= b;
                    case GE -> a >= b;
                    case EQ -> a == b;
                    default -> a != b;
                };
            }
            default -> {
            }
        }
        Object a = convert(l.value(), rt, c);
        Object b = convert(r.value(), rt, c);
        if (a instanceof Integer x && b instanceof Integer y) {
            return switch (kind) {
                case ADD -> x + y;
                case SUB -> x - y;
                case MUL -> x * y;
                case DIV -> x / y;
                case MOD -> x % y;
                case AND -> x & y;
                case OR -> x | y;
                case XOR -> x ^ y;
                default -> throw new ClassCastException();
            };
        } else if (a instanceof Long x && b instanceof Long y) {
            return switch (kind) {
                case ADD -> x + y;
                case SUB -> x - y;
                case MUL -> x * y;
                case DIV -> x / y;
                case MOD -> x % y;
                case AND -> x & y;
                case OR -> x | y;
                case XOR -> x ^ y;
                default -> throw new ClassCastException();
            };
        } else if (a instanceof Float x && b instanceof Float y) {
            return switch (kind) {
                case ADD -> x + y;
                case SUB -> x - y;
                case MUL -> x * y;
                case DIV -> x / y;
                case MOD -> x % y;
                default -> throw new ClassCastException();
            };
        } else if (a instanceof Double x && b instanceof Double y) {
            return switch (kind) {
                case ADD -> x + y;
                case SUB -> x - y;
                case MUL -> x * y;
                case DIV -> x / y;
                case MOD -> x % y;
                default -> throw new ClassCastException();
            };
        }
        throw new ClassCastException();
    }

    private static Object display(Value v) {
        if (v.value() instanceof Integer i && v.type() instanceof JPrimitiveType p && p.name().equals("char")) {
            return (char) i.intValue();
        }
        return v.value();
    }

    public static Object convert(Object value, JType type, JTypeConstants c) {
        if (!(type instanceof JPrimitiveType p)) {
            return value;
        }
        if (value instanceof Boolean b) {
            if (!p.name().equals("boolean")) {
                throw new ClassCastException();
            }
            return b;
        }
        if (p.name().equals("boolean")) {
            if (value instanceof Integer i) {
                return i != 0;
            }
            throw new ClassCastException();
        }
        long l;
        double d;
        boolean floating;
        if (value instanceof Character ch) {
            l = ch;
            d = ch;
            floating = false;
        } else if (value instanceof Number n) {
            floating = n instanceof Float || n instanceof Double;
            l = n.longValue();
            d = n.doubleValue();
        } else {
            throw new ClassCastException();
        }
        // JLS §5.1.2, §5.1.3 primitive widening and narrowing conversions
        return switch (p.name()) {
            case "byte" -> floating ? (int) (byte) (int) d : (int) (byte) l;
            case "short" -> floating ? (int) (short) (int) d : (int) (short) l;
            case "char" -> floating ? (int) (char) (int) d : (int) (char) l;
            case "int" -> floating ? (int) d : (int) l;
            case "long" -> floating ? (long) d : l;
            case "float" -> floating ? (float) d : (float) l;
            case "double" -> floating ? d : (double) l;
            default -> throw new ClassCastException();
        };
    }
}
