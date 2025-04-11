package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JRecursiveTypeVisitor<R, C> implements JTypeVisitor<List<R>, Map<JType, List<R>>> {
    private JTypeVisitor<R, C> visitor;
    private boolean visitStructural;
    private C context;

    public static class Box<T> {
        public T val;
    }

    public JRecursiveTypeVisitor(JTypeVisitor<R, C> visitor, C context, boolean visitStructural) {
        this.visitor = visitor;
        this.context = context;
        this.visitStructural = visitStructural;
    }

    @Override
    public List<R> visitClassType(JClassType type, Map<JType, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitClassType(type, this.context);
        if (r != null) {
            result.add(r);
        }

        if (type instanceof JParameterizedClassType ct) {
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
    public List<R> visitPrimitiveType(JPrimitiveType type, Map<JType, List<R>> context) {
        R r = this.visitor.visitPrimitiveType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<R> visitWildcardType(JWildType type, Map<JType, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitWildcardType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        if (type instanceof JWildType.Upper wtu) {
            wtu.upperBounds().forEach(t -> result.addAll(this.visit(t, context)));
        } else if (type instanceof JWildType.Lower wtl) {
            wtl.lowerBounds().forEach(t -> result.addAll(this.visit(t, context)));
        }
        return result;
    }

    @Override
    public List<R> visitArrayType(JArrayType type, Map<JType, List<R>> context) {
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
    public List<R> visitIntersectionType(JIntersectionType type, Map<JType, List<R>> context) {
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
    public List<R> visitMethodType(JMethodType type, Map<JType, List<R>> context) {
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

        if (type instanceof JParameterizedMethodType mt) {
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
    public List<R> visitVarType(JVarType type, Map<JType, List<R>> context) {
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
    public List<R> visitMetaVarType(JMetaVarType type, Map<JType, List<R>> context) {
        R r = this.visitor.visitMetaVarType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<R> visitNoneType(JNoneType type, Map<JType, List<R>> context) {
        R r = this.visitor.visitNoneType(type, this.context);
        if (r != null) {
            return Collections.singletonList(r);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<R> visitFieldType(JFieldReference type, Map<JType, List<R>> context) {
        if (context.containsKey(type)) return context.get(type);
        List<R> result = new ArrayList<>();
        context.put(type, result);

        R r = this.visitor.visitFieldType(type, this.context);
        if (r != null) {
            result.add(r);
        }
        result.addAll(this.visit(type.type(), context));

        if (this.visitStructural) {
            result.addAll(this.visit(type.outerClass(), context));
        }

        return result;
    }
}
