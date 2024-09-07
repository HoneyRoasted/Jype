package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.Set;
import java.util.stream.Collectors;

public class JSimpleToStringVisitor implements JToStringVisitor {
    @Override
    public String classToString(JClassType type, Set<JType> context) {
        if (type instanceof JParameterizedClassType pct && pct.hasTypeArguments()) {
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
    public String wildcardToString(JWildType type, Set<JType> context) {
        if (type instanceof JWildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context))
                        .collect(Collectors.joining(", "));
            }
        } else if (type instanceof JWildType.Lower) {
            return "? super " + type.lowerBounds().stream().map(t -> visit(t, context))
                    .collect(Collectors.joining(", "));
        }
        return "?";
    }

    @Override
    public String arrayToString(JArrayType type, Set<JType> context) {
        return this.visit(type.component(), context) + "[]";
    }

    @Override
    public String intersectionToString(JIntersectionType type, Set<JType> context) {
        return type.children().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & "));
    }

    @Override
    public String methodToString(JMethodType type, Set<JType> context) {
        if (type instanceof JParameterizedMethodType pmt && pmt.hasTypeArguments()) {
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
    public String varToString(JVarType type, Set<JType> context) {
        return type.name();
    }

    @Override
    public String primToString(JPrimitiveType type, Set<JType> context) {
        return type.name();
    }

    @Override
    public String metaVarToString(JMetaVarType type, Set<JType> context) {
        return "#" + type.name() + ":" + Integer.toString(type.identity(), 16);
    }

    @Override
    public String noneToString(JNoneType type, Set<JType> context) {
        return "@" + type.name();
    }
}
