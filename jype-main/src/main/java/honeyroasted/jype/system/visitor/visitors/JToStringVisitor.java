package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.HashSet;
import java.util.Set;

public interface JToStringVisitor extends JTypeVisitor<String, Set<JType>> {

    @Override
    default String visit(JType type) {
        return visit(type, new HashSet<>());
    }

    @Override
    default String visitClassType(JClassType type, Set<JType> context) {
        if (context.contains(type)) return "...";
        context = JType.concat(context, type);

        return classToString(type, context);
    }

    String classToString(JClassType type, Set<JType> context);

    @Override
    default String visitWildcardType(JWildType type, Set<JType> context) {
        if (context.contains(type)) return "?...";
        context = JType.concat(context, type);

        return wildcardToString(type, context);
    }

    String wildcardToString(JWildType type, Set<JType> context);

    @Override
    default String visitArrayType(JArrayType type, Set<JType> context) {
        if (context.contains(type)) return "...";
        context = JType.concat(context, type);

        return arrayToString(type, context);
    }

    String arrayToString(JArrayType type, Set<JType> context);

    @Override
    default String visitIntersectionType(JIntersectionType type, Set<JType> context) {
        if (context.contains(type)) return "...";
        context = JType.concat(context, type);

        return intersectionToString(type, context);
    }

    String intersectionToString(JIntersectionType type, Set<JType> context);

    @Override
    default String visitMethodType(JMethodType type, Set<JType> context) {
        if (context.contains(type)) return "...";
        context = JType.concat(context, type);

        return methodToString(type, context);
    }

    String methodToString(JMethodType type, Set<JType> context);

    @Override
    default String visitVarType(JVarType type, Set<JType> context) {
        if (context.contains(type)) return type.name();
        context = JType.concat(context, type);

        return varToString(type, context);
    }

    String varToString(JVarType type, Set<JType> context);

    @Override
    default String visitPrimitiveType(JPrimitiveType type, Set<JType> context) {
        if (context.contains(type)) return type.name();
        context = JType.concat(context, type);

        return primToString(type, context);
    }

    String primToString(JPrimitiveType type, Set<JType> context);

    @Override
    default String visitMetaVarType(JMetaVarType type, Set<JType> context) {
        if (context.contains(type)) return type.name();
        context = JType.concat(context, type);

        return metaVarToString(type, context);
    }

    String metaVarToString(JMetaVarType type, Set<JType> context);

    @Override
    default String visitNoneType(JNoneType type, Set<JType> context) {
        if (context.contains(type)) return type.name();
        context = JType.concat(context, type);

        return this.noneToString(type, context);
    }

    String noneToString(JNoneType type, Set<JType> context);
}
