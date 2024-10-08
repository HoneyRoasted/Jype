package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JSignature;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

public class JToSignatureTypeVisitor implements JTypeVisitor<JSignature, JToSignatureTypeVisitor.Mode> {

    @Override
    public JSignature visit(JType type) {
        return this.visit(type, Mode.USAGE);
    }

    @Override
    public JSignature visitClassType(JClassType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context.useDelims()) sb.append("L");

        if (context.isDeclaration()) {
            if (type.hasTypeParameters()) {
                sb.append("<");
                type.typeParameters().forEach(t -> sb.append(this.visit(t, Mode.DECLARATION)));
                sb.append(">");
            }

            if (type.superClass() != null) {
                sb.append(this.visit(type.superClass(), Mode.USAGE));
            }

            type.interfaces().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
            if (context.useDelims()) sb.append(";");

            return new JSignature.ClassDeclaration(sb.toString());
        } else {
            JClassType outerType = type instanceof JParameterizedClassType pct ? pct.outerType() : type.outerClass();
            if (outerType != null && !Modifier.isStatic(type.modifiers()) && outerType.hasTypeArguments()) {
                sb.append(this.visit(outerType, Mode.USAGE_NO_DELIMS));
                sb.append(".").append(type.namespace().name().value());
            } else {
                sb.append(type.namespace().location().toInternalName());
            }

            if (type.hasTypeArguments()) {
                sb.append("<");
                type.typeArguments().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
                sb.append(">");
            }
            if (context.useDelims()) sb.append(";");

            return new JSignature.JType(sb.toString());
        }
    }

    @Override
    public JSignature visitMethodType(JMethodType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context.isDeclaration()) {
            if (type.hasTypeParameters()) {
                sb.append("<");
                type.typeParameters().forEach(t -> sb.append(this.visit(t, Mode.DECLARATION)));
                sb.append(">");
            }
            sb.append("(");
            type.parameters().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
            sb.append(")");
            sb.append(this.visit(type.returnType(), Mode.USAGE));
            type.exceptionTypes().forEach(t -> sb.append("^").append(this.visit(t, Mode.USAGE)));

            return new JSignature.MethodDeclaration(sb.toString());
        } else {
            sb.append("(");
            type.parameters().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
            sb.append(")");
            sb.append(this.visit(type.returnType(), Mode.USAGE));

            return new JSignature.JMethodReference(sb.toString());
        }
    }

    @Override
    public JSignature visitWildcardType(JWildType type, Mode context) {
        if (type instanceof JWildType.Upper wtu) {
            if ((wtu.upperBounds().size() == 1 && type.typeSystem().constants().object().equals(wtu.upperBounds().iterator().next())) ||
                    wtu.upperBounds().isEmpty()) {
                return new JSignature.JType("*");
            }
            return new JSignature.JType("+" + this.visit(wtu.upperBounds().iterator().next(), Mode.USAGE));
        } else if (type instanceof JWildType.Lower wtl) {
            return new JSignature.JType("-" + this.visit(wtl.lowerBounds().iterator().next(), Mode.USAGE));
        }
        return new JSignature.JType("*");
    }

    @Override
    public JSignature visitVarType(JVarType type, Mode context) {
        if (context.isDeclaration()) {
            return new JSignature.JType(type.name() + ":" + (type.upperBounds().size() > 1 ? ":" : "") +
                    type.upperBounds().stream().map(t -> visit(t, Mode.USAGE).value()).collect(Collectors.joining(":")));
        } else {
            return new JSignature.JType("T" + type.name() + ";");
        }
    }

    @Override
    public JSignature visitMetaVarType(JMetaVarType type, Mode context) {
        return new JSignature.JType("T" + type.name() + ";");
    }

    @Override
    public JSignature visitArrayType(JArrayType type, Mode context) {
        return new JSignature.JType("[" + visit(type.component(), context));
    }

    @Override
    public JSignature visitIntersectionType(JIntersectionType type, Mode context) {
        return new JSignature.JType(type.children().stream().map(t -> this.visit(t, context).value()).collect(Collectors.joining(":")));
    }

    @Override
    public JSignature visitPrimitiveType(JPrimitiveType type, Mode context) {
        return new JSignature.JType(type.descriptor());
    }

    @Override
    public JSignature visitNoneType(JNoneType type, Mode context) {
        return new JSignature.JType("V");
    }

    public enum Mode {
        DECLARATION {
            @Override
            boolean isUsage() {
                return false;
            }

            @Override
            boolean useDelims() {
                return false;
            }
        },
        USAGE {
            @Override
            boolean isUsage() {
                return true;
            }

            @Override
            boolean useDelims() {
                return true;
            }
        },
        USAGE_NO_DELIMS {
            @Override
            boolean isUsage() {
                return true;
            }

            @Override
            boolean useDelims() {
                return false;
            }
        };

        boolean isDeclaration() {
            return !isUsage();
        }

        abstract boolean isUsage();

        abstract boolean useDelims();
    }

}
