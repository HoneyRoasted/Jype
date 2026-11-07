package honeyroasted.jype.system.expression;

import honeyroasted.almonds.SimpleName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JExpressionSubstitution {
    private JExpressionSubstitution() {
    }

    public static JExpressionInformation substitute(JExpressionInformation expr, Map<JExpressionInformation.LambdaParameter, ? extends JExpressionInformation> replacements) {
        if (replacements.isEmpty() || expr == null) {
            return expr;
        }
        return switch (expr) {
            case JExpressionInformation.LambdaParameter p -> replacements.containsKey(p) ? replacements.get(p) : p;
            case JExpressionInformation.Instantiation.Simple i ->
                    new JExpressionInformation.Instantiation.Simple(i.declaring(), i.declaringMethod(), i.type(), list(i.parameters(), replacements), i.explicitTypeArguments());
            case JExpressionInformation.Instantiation.Full i ->
                    new JExpressionInformation.Instantiation.Full(i.declaring(), i.declaringMethod(), i.type(), list(i.parameters(), replacements), i.explicitTypeArguments(), i.raw(), i.constructorTypeArguments(), substitute(i.outerInstance(), replacements));
            case JExpressionInformation.MethodInvocation.Simple<?> m ->
                    new JExpressionInformation.MethodInvocation.Simple<>(m.declaring(), m.declaringMethod(), source(m.source(), replacements), m.name(), list(m.parameters(), replacements), m.explicitTypeArguments());
            case JExpressionInformation.GetField.Simple<?> g ->
                    new JExpressionInformation.GetField.Simple<>(g.declaring(), g.declaringMethod(), source(g.source(), replacements), g.name());
            case JExpressionInformation.Lambda.Simple l -> new JExpressionInformation.Lambda.Simple(l.parameters(), l.explicitParameterTypes(), body(l.lambdaBody(), replacements));
            case JExpressionInformation.InvocationReference.Simple<?> r ->
                    new JExpressionInformation.InvocationReference.Simple<>(source(r.source(), replacements), r.methodName(), r.explicitTypeArguments());
            case JExpressionInformation.Cast.Simple c -> new JExpressionInformation.Cast.Simple(substitute(c.operand(), replacements), c.castType());
            case JExpressionInformation.InstanceOf.Simple i -> new JExpressionInformation.InstanceOf.Simple(substitute(i.operand(), replacements), i.checkedType());
            case JExpressionInformation.UnaryOperator.Simple u -> new JExpressionInformation.UnaryOperator.Simple(u.kind(), substitute(u.operand(), replacements));
            case JExpressionInformation.BinaryOperator.Simple b ->
                    new JExpressionInformation.BinaryOperator.Simple(b.kind(), substitute(b.left(), replacements), substitute(b.right(), replacements));
            case JExpressionInformation.Conditional.Simple c ->
                    new JExpressionInformation.Conditional.Simple(substitute(c.condition(), replacements), substitute(c.whenTrue(), replacements), substitute(c.whenFalse(), replacements));
            case JExpressionInformation.ArrayCreation.Simple a ->
                    new JExpressionInformation.ArrayCreation.Simple(a.componentType(), list(a.dimensions(), replacements), a.extraDimensions(), substitute(a.initializer(), replacements));
            case JExpressionInformation.ArrayInitializer.Simple a -> new JExpressionInformation.ArrayInitializer.Simple(list(a.elements(), replacements));
            default -> expr;
        };
    }

    public static JExpressionInformation.LambdaBody body(JExpressionInformation.LambdaBody body, Map<JExpressionInformation.LambdaParameter, ? extends JExpressionInformation> replacements) {
        if (body instanceof JExpressionInformation.LambdaBody.Expression e) {
            return new JExpressionInformation.LambdaBody.Expression(substitute(e.expression(), replacements));
        }
        JExpressionInformation.LambdaBody.Block b = (JExpressionInformation.LambdaBody.Block) body;
        return new JExpressionInformation.LambdaBody.Block(list(b.statements(), replacements), list(b.returnExpressions(), replacements), b.hasVoidReturn(), b.canCompleteNormally());
    }

    private static List<JExpressionInformation> list(List<JExpressionInformation> exprs, Map<JExpressionInformation.LambdaParameter, ? extends JExpressionInformation> replacements) {
        List<JExpressionInformation> result = new ArrayList<>();
        for (JExpressionInformation e : exprs) {
            result.add(substitute(e, replacements));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends SimpleName> T source(T source, Map<JExpressionInformation.LambdaParameter, ? extends JExpressionInformation> replacements) {
        if (source instanceof JExpressionInformation e) {
            return (T) substitute(e, replacements);
        }
        return source;
    }
}
