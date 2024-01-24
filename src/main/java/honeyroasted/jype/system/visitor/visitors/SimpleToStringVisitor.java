package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.Set;
import java.util.stream.Collectors;

public class SimpleToStringVisitor implements ToStringVisitor {
    @Override
    public String classToString(ClassType type, Set<Type> context) {
        if (type instanceof ParameterizedClassType pct && pct.hasTypeArguments()) {
            if (pct.hasRelevantOuterType()) {
                return visit(pct.outerType(), context) + "." + pct.namespace().name().value() + "<" +
                        pct.typeArguments().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ">";
            } else {
                return pct.namespace().name().simpleName() + "<" +
                        pct.typeArguments().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ">";
            }
        } else {
            if (type.hasRelevantOuterType()) {
                return visit(type.outerType(), context) + "." + type.namespace().name().value();
            } else {
                return type.namespace().name().simpleName();
            }
        }
    }

    @Override
    public String wildcardToString(WildType type, Set<Type> context) {
        if (type instanceof WildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context))
                        .collect(Collectors.joining(", "));
            }
        } else if (type instanceof WildType.Lower) {
            return "? super " + type.lowerBounds().stream().map(t -> visit(t, context))
                    .collect(Collectors.joining(", "));
        }
        return "?";
    }

    @Override
    public String arrayToString(ArrayType type, Set<Type> context) {
        return this.visit(type.component(), context) + "[]";
    }

    @Override
    public String intersectionToString(IntersectionType type, Set<Type> context) {
        return type.children().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & "));
    }

    @Override
    public String methodToString(MethodType type, Set<Type> context) {
        if (type instanceof ParameterizedMethodType pmt && pmt.hasTypeArguments()) {
            if (pmt.hasRelevantOuterType()) {
                return visit(type.outerType(), context) + "." + type.location().name() + "<" + type.typeParameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ">(" + type.parameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ") -> " + visit(type.returnType(), context);
            } else {
                return type.location().simpleName() + "<" + type.typeParameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ">(" + type.parameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ") -> " + visit(type.returnType(), context);
            }
        } else {
            if (type.hasRelevantOuterType()) {
                return visit(type.outerType(), context) + "." + type.location().name() + "("
                        + type.parameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ") -> " + visit(type.returnType(), context);
            } else {
                return type.location().simpleName() + "("
                        + type.parameters().stream().map(t -> visit(t, context)).collect(Collectors.joining(", ")) +
                        ") -> " + visit(type.returnType(), context);
            }
        }
    }

    @Override
    public String varToString(VarType type, Set<Type> context) {
        return type.name();
    }

    @Override
    public String primToString(PrimitiveType type, Set<Type> context) {
        return type.name();
    }

    @Override
    public String metaVarToString(MetaVarType type, Set<Type> context) {
        return "#" + type.name() + ":" + Integer.toString(type.identity(), 36);
    }

    @Override
    public String noneToString(NoneType type, Set<Type> context) {
        return "@" + type.name();
    }
}
