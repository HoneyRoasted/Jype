package honeyroasted.jype.fuzz;

import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JInstantiableType;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface FuzzExpr {
    String source();

    JExpressionInformation toJype(FuzzContext ctx);

    record TypeRef(String source, String holderField) {
        public JType resolve(FuzzContext ctx) {
            return ctx.typeOf(this.holderField);
        }
    }

    private static List<JArgumentType> args(FuzzContext ctx, List<TypeRef> refs) {
        List<JArgumentType> result = new ArrayList<>();
        for (TypeRef ref : refs) {
            result.add((JArgumentType) ref.resolve(ctx));
        }
        return result;
    }

    private static String typeArgs(List<TypeRef> refs) {
        return refs.isEmpty() ? "" : "<" + refs.stream().map(TypeRef::source).collect(Collectors.joining(", ")) + ">";
    }

    record Field(String name) implements FuzzExpr {
        @Override
        public String source() {
            return "M." + this.name;
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.GetField.Simple<>(ctx.host(), null, ctx.host(), this.name);
        }
    }

    record Literal(String source, Object value) implements FuzzExpr {
        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.Constant.Simple(this.value);
        }
    }

    record StaticCall(String name, List<TypeRef> typeArguments, List<FuzzExpr> arguments) implements FuzzExpr {
        @Override
        public String source() {
            return "M." + typeArgs(this.typeArguments) + this.name + "(" + this.arguments.stream().map(FuzzExpr::source).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.MethodInvocation.Simple<>(ctx.host(), null, ctx.host(), this.name,
                    this.arguments.stream().map(a -> a.toJype(ctx)).toList(), args(ctx, this.typeArguments));
        }
    }

    record InstanceCall(FuzzExpr receiver, String name, List<FuzzExpr> arguments) implements FuzzExpr {
        @Override
        public String source() {
            return "(" + this.receiver.source() + ")." + this.name + "(" + this.arguments.stream().map(FuzzExpr::source).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.MethodInvocation.Simple<>(ctx.host(), null, this.receiver.toJype(ctx), this.name,
                    this.arguments.stream().map(a -> a.toJype(ctx)).toList(), Collections.emptyList());
        }
    }

    record New(String className, List<TypeRef> typeArguments, boolean diamond, List<FuzzExpr> arguments) implements FuzzExpr {
        @Override
        public String source() {
            return "new " + this.className + (this.diamond ? "<>" : typeArgs(this.typeArguments)) + "(" + this.arguments.stream().map(FuzzExpr::source).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.Instantiation.Simple(ctx.host(), null, ctx.classNamed(this.className),
                    this.arguments.stream().map(a -> a.toJype(ctx)).toList(), args(ctx, this.typeArguments));
        }
    }

    ThreadLocal<java.util.Deque<java.util.Map<String, JExpressionInformation.LambdaParameter>>> SCOPES = ThreadLocal.withInitial(java.util.ArrayDeque::new);

    record LambdaParam(String name) implements FuzzExpr {
        @Override
        public String source() {
            return this.name;
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            for (java.util.Map<String, JExpressionInformation.LambdaParameter> scope : SCOPES.get()) {
                if (scope.containsKey(this.name)) {
                    return scope.get(this.name);
                }
            }
            throw new IllegalStateException("unbound lambda parameter " + this.name);
        }
    }

    record Lambda(List<String> names, List<TypeRef> explicitTypes, FuzzExpr body, int form) implements FuzzExpr {
        @Override
        public String source() {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < this.names.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (this.explicitTypes != null) {
                    sb.append(this.explicitTypes.get(i).source()).append(" ");
                }
                sb.append(this.names.get(i));
            }
            sb.append(") -> ");
            return switch (this.form) {
                case 1 -> sb.append("{ return ").append(this.body.source()).append("; }").toString();
                case 2 -> sb.append("{ ").append(this.body.source()).append("; }").toString();
                default -> sb.append(this.body.source()).toString();
            };
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            List<JExpressionInformation.LambdaParameter> params = new ArrayList<>();
            java.util.Map<String, JExpressionInformation.LambdaParameter> scope = new java.util.HashMap<>();
            for (String n : this.names) {
                JExpressionInformation.LambdaParameter p = JExpressionInformation.LambdaParameter.fresh(n);
                params.add(p);
                scope.put(n, p);
            }
            SCOPES.get().push(scope);
            try {
                JExpressionInformation b = this.body.toJype(ctx);
                JExpressionInformation.LambdaBody body = switch (this.form) {
                    case 1 -> new JExpressionInformation.LambdaBody.Block(List.of(), List.of(b), false, false);
                    case 2 -> new JExpressionInformation.LambdaBody.Block(List.of(b), List.of(), false, true);
                    default -> new JExpressionInformation.LambdaBody.Expression(b);
                };
                return new JExpressionInformation.Lambda.Simple(params, this.explicitTypes == null ? null : this.explicitTypes.stream().map(t -> t.resolve(ctx)).toList(), body);
            } finally {
                SCOPES.get().pop();
            }
        }
    }

    record StaticRef(String className, String name) implements FuzzExpr {
        @Override
        public String source() {
            return this.className + "::" + this.name;
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.InvocationReference.Simple<>(ctx.classNamed(this.className), this.name, Collections.emptyList());
        }
    }

    record CtorRef(TypeRef type) implements FuzzExpr {
        @Override
        public String source() {
            return this.type.source() + "::new";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            JType t = this.type.resolve(ctx);
            return new JExpressionInformation.InstantiationReference.Simple((JInstantiableType) (t instanceof honeyroasted.jype.type.JClassType ct && !(t instanceof honeyroasted.jype.type.JArrayType) ? ct.classReference() : t), Collections.emptyList());
        }
    }

    record Cast(TypeRef type, FuzzExpr operand) implements FuzzExpr {
        @Override
        public String source() {
            return "((" + this.type.source() + ") " + this.operand.source() + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.Cast.Simple(this.operand.toJype(ctx), this.type.resolve(ctx));
        }
    }

    record Conditional(FuzzExpr condition, FuzzExpr whenTrue, FuzzExpr whenFalse) implements FuzzExpr {
        @Override
        public String source() {
            return "(" + this.condition.source() + " ? " + this.whenTrue.source() + " : " + this.whenFalse.source() + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.Conditional.Simple(this.condition.toJype(ctx), this.whenTrue.toJype(ctx), this.whenFalse.toJype(ctx));
        }
    }

    record Binary(JExpressionInformation.BinaryOperator.Kind kind, String symbol, FuzzExpr left, FuzzExpr right) implements FuzzExpr {
        @Override
        public String source() {
            return "(" + this.left.source() + " " + this.symbol + " " + this.right.source() + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.BinaryOperator.Simple(this.kind, this.left.toJype(ctx), this.right.toJype(ctx));
        }
    }

    record Unary(JExpressionInformation.UnaryOperator.Kind kind, String symbol, FuzzExpr operand) implements FuzzExpr {
        @Override
        public String source() {
            return "(" + this.symbol + this.operand.source() + ")";
        }

        @Override
        public JExpressionInformation toJype(FuzzContext ctx) {
            return new JExpressionInformation.UnaryOperator.Simple(this.kind, this.operand.toJype(ctx));
        }
    }
}
