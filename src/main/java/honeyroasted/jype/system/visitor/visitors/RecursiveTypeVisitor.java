package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecursiveTypeVisitor<R, C> implements TypeVisitor<List<R>, Map<Type, List<R>>> {
    private TypeVisitor<R, C> visitor;
    private boolean visitStructural;
    private C context;

    public static class Box<T> { public T val; }

    public RecursiveTypeVisitor(TypeVisitor<R, C> visitor, C context, boolean visitStructural) {
        this.visitor = visitor;
        this.context = context;
        this.visitStructural = visitStructural;
    }

    @Override
    public List<R> visitClassType(ClassType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitClassType(type, this.context);
        if (r != null) {
            result.add(r);
        }

        if (type instanceof ParameterizedClassType ct) {
            if (ct.outerType() != null && (this.visitStructural || !Modifier.isStatic(ct.modifiers()))) {
                result.addAll(this.visit(ct.outerClass(), context));
            }
        }

        if (this.visitStructural) {
            if (type.outerClass() != null) {
                result.addAll(this.visit(type.outerClass(), context));
            }

            if (type.superClass() != null) {
                result.addAll(this.visit(type.superClass(), context));
            }

            type.interfaces().forEach(t -> result.addAll(this.visit(t, context)));
            type.typeParameters().forEach(t -> result.addAll(this.visit(t, context)));
        }

        type.typeArguments().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitPrimitiveType(PrimitiveType type, Map<Type, List<R>> context) {
        R r = this.visitor.visitPrimitiveType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<R> visitWildcardType(WildType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitWildcardType(type, this.context);
        if (r != null) {
            result.add(r);
        }
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

        R r = this.visitor.visitArrayType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        result.addAll(this.visit(type.component(), context));
        return result;
    }

    @Override
    public List<R> visitIntersectionType(IntersectionType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitIntersectionType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        type.children().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitMethodType(MethodType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitMethodType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        result.addAll(this.visit(type.returnType(), context));
        type.parameters().forEach(t -> result.addAll(this.visit(t, context)));
        type.exceptionTypes().forEach(t -> result.addAll(this.visit(t, context)));
        type.typeArguments().forEach(t -> result.addAll(this.visit(t, context)));

        if (type instanceof ParameterizedMethodType mt) {
            if (mt.outerType() != null && (this.visitStructural || !Modifier.isStatic(mt.modifiers()))) {
                result.addAll(this.visit(mt.outerType(), context));
            }
        }

        if (this.visitStructural) {
            type.typeParameters().forEach(t -> result.addAll(this.visit(t, context)));
            if (type.outerClass() != null) {
                result.addAll(this.visit(type.outerClass(), context));
            }
        }

        return result;
    }

    @Override
    public List<R> visitVarType(VarType type, Map<Type, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitVarType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        type.upperBounds().forEach(t -> result.addAll(this.visit(t, context)));
        return result;
    }

    @Override
    public List<R> visitMetaVarType(MetaVarType type, Map<Type, List<R>> context) {
        R r = this.visitor.visitMetaVarType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<R> visitNoneType(NoneType type, Map<Type, List<R>> context) {
        R r = this.visitor.visitNoneType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }
}
