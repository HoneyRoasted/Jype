package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.Set;

public interface ToStringVisitor extends TypeVisitor<String, Set<Type>> {

    @Override
    default String visitClassType(ClassType type, Set<Type> context) {
        if (context.contains(type)) return "...";
        context = Type.concat(context, type);
        
        return classToString(type, context);
    }

    String classToString(ClassType type, Set<Type> context);

    @Override
    default String visitWildcardType(WildType type, Set<Type> context) {
        if (context.contains(type)) return "?...";
        context = Type.concat(context, type);
        
        return wildcardToString(type, context);
    }

    String wildcardToString(WildType type, Set<Type> context);

    @Override
    default String visitArrayType(ArrayType type, Set<Type> context) {
        if (context.contains(type)) return "...";
        context = Type.concat(context, type);
        
        return "[" + visit(type.component(), context) + "]";
    }

    String arrayToString(ArrayType type, Set<Type> context);

    @Override
    default String visitIntersectionType(IntersectionType type, Set<Type> context) {
        if (context.contains(type)) return "...";
        context = Type.concat(context, type);
        
        return intersectionToString(type, context);
    }

    String intersectionToString(IntersectionType type, Set<Type> context);

    @Override
    default String visitMethodType(MethodType type, Set<Type> context) {
        if (context.contains(type)) return "...";
        context = Type.concat(context, type);
        
        return methodToString(type, context);
    }

    String methodToString(MethodType type, Set<Type> context);

    @Override
    default String visitVarType(VarType type, Set<Type> context) {
        if (context.contains(type)) return type.name();
        context = Type.concat(context, type);

        return varToString(type, context);
    }

    String varToString(VarType type, Set<Type> context);

    @Override
    default String visitPrimitiveType(PrimitiveType type, Set<Type> context) {
        if (context.contains(type)) return type.name();
        context = Type.concat(context, type);

        return primToString(type, context);
    }

    String primToString(PrimitiveType type, Set<Type> context);

    @Override
    default String visitMetaVarType(MetaVarType type, Set<Type> context) {
        if (context.contains(type)) return type.name();
        context = Type.concat(context, type);

        return metaVarToString(type, context);
    }

    String metaVarToString(MetaVarType type, Set<Type> context);

    @Override
    default String visitNoneType(NoneType type, Set<Type> context) {
        if (context.contains(type)) return type.name();
        context = Type.concat(context, type);

        return this.noneToString(type, context);
    }

    String noneToString(NoneType type, Set<Type> context);
}
