package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.signature.Signature;

import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

public class SignatureTypeVisitor implements TypeVisitor<Signature, SignatureTypeVisitor.Mode> {

    @Override
    public Signature visitClassType(ClassType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context.useDelims()) sb.append("L");

        if (context == Mode.DECLARATION) {
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

            return new Signature.Class(sb.toString());
        } else {
            ClassType outerType = type instanceof ParameterizedClassType pct ? pct.outerType() : type.outerClass();
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

            return new Signature.Type(sb.toString());
        }
    }

    @Override
    public Signature visitMethodType(MethodType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context == Mode.DECLARATION) {
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

            return new Signature.Method(sb.toString());
        } else {
            sb.append("(");
            type.parameters().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
            sb.append(")");
            sb.append(this.visit(type.returnType(), Mode.USAGE));

            return new Signature.Type(sb.toString());
        }
    }

    @Override
    public Signature visitWildcardType(WildType type, Mode context) {
        if (type instanceof WildType.Upper wtu) {
            if ((wtu.upperBounds().size() == 1 && type.typeSystem().constants().object().equals(wtu.upperBounds().iterator().next())) ||
                    wtu.upperBounds().isEmpty()) {
                return new Signature.Type("*");
            }
            return new Signature.Type("+" + this.visit(wtu.upperBounds().iterator().next(), Mode.USAGE));
        } else if (type instanceof WildType.Lower wtl) {
            return new Signature.Type("-" + this.visit(wtl.lowerBounds().iterator().next(), Mode.USAGE));
        }
        return new Signature.Type("*");
    }

    @Override
    public Signature visitVarType(VarType type, Mode context) {
        if (context == Mode.DECLARATION) {
            return new Signature.Type(type.name() + ":" +
                    type.upperBounds().stream().map(t -> visit(t, Mode.USAGE).value()).collect(Collectors.joining(":")));
        } else {
            return new Signature.Type("T" + type.name() + ";");
        }
    }

    @Override
    public Signature visitMetaVarType(MetaVarType type, Mode context) {
        return new Signature.Type("T" + type.name() + ";");
    }

    @Override
    public Signature visitArrayType(ArrayType type, Mode context) {
        return new Signature.Type("[" + visit(type, context));
    }

    @Override
    public Signature visitIntersectionType(IntersectionType type, Mode context) {
        return new Signature.Type(type.children().stream().map(t -> this.visit(t, context).value()).collect(Collectors.joining(":")));
    }

    @Override
    public Signature visitPrimitiveType(PrimitiveType type, Mode context) {
        return new Signature.Type(type.descriptor());
    }

    @Override
    public Signature visitNoneType(NoneType type, Mode context) {
        return new Signature.Type("V");
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

        abstract boolean isUsage();

        abstract boolean useDelims();
    }

}
