package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecursiveTypeVisitor<R, C> implements TypeVisitor<List<R>, Map<Type, List<R>>> {
    private TypeVisitor<R, C> visitor;
    private C context;

    public RecursiveTypeVisitor(TypeVisitor<R, C> visitor, C context) {
        this.visitor = visitor;
        this.context = context;
    }

    @Override
    public List<R> visitClassType(ClassType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        result.add(this.visitor.visitClassType(type, this.context));

        if (type.outerClass() != null) {
            result.addAll(this.visit(type.outerClass(), context));
        }

        if (type.superClass() != null) {
            result.addAll(this.visit(type.superClass(), context));
        }

        type.interfaces().forEach(t -> result.addAll(this.visit(t, context)));
        type.typeParameters().forEach(t -> result.addAll(this.visit(t, context)));
        type.typeArguments().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitPrimitiveType(PrimitiveType type, Map<Type, List<R>> context) {
        return List.of(this.visitor.visitPrimitiveType(type, this.context));
    }

    @Override
    public List<R> visitWildcardType(WildType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        result.add(this.visitor.visitWildcardType(type, this.context));
        if (type instanceof WildType.Upper wtu) {
            wtu.upperBounds().forEach(t -> result.addAll(this.visit(t, context)));
        } else if (type instanceof WildType.Lower wtl) {
            wtl.lowerBounds().forEach(t -> result.addAll(this.visit(t, context)));
        }
        return result;
    }

    @Override
    public List<R> visitArrayType(ArrayType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        result.add(this.visitor.visitArrayType(type, this.context));
        result.addAll(this.visit(type.component(), context));
        return result;
    }

    @Override
    public List<R> visitMethodType(MethodType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        result.add(this.visitor.visitMethodType(type, this.context));
        result.addAll(this.visit(type.returnType(), context));
        type.parameters().forEach(t -> result.addAll(this.visit(t, context)));
        type.exceptionTypes().forEach(t -> result.addAll(this.visit(t, context)));
        type.typeParameters().forEach(t -> result.addAll(this.visit(t, context)));
        type.typeArguments().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitVarType(VarType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        result.add(this.visitor.visitVarType(type, this.context));
        type.upperBounds().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitMetaVarType(MetaVarType type, Map<Type, List<R>> context) {
        return List.of(this.visitor.visitMetaVarType(type, this.context));
    }

    @Override
    public List<R> visitNoneType(NoneType type, Map<Type, List<R>> context) {
        return List.of(this.visitor.visitNoneType(type, this.context));
    }
}
