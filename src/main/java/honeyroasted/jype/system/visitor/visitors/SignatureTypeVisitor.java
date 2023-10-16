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

import java.lang.reflect.Modifier;
import java.util.stream.Collectors;

public class SignatureTypeVisitor implements TypeVisitor<String, SignatureTypeVisitor.Mode> {

    @Override
    public String visitClassType(ClassType type, Mode context) {
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
        }

        if (context.useDelims()) sb.append(";");
        return sb.toString();
    }

    @Override
    public String visitMethodType(MethodType type, Mode context) {
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
        } else {
            sb.append("(");
            type.parameters().forEach(t -> sb.append(this.visit(t, Mode.USAGE)));
            sb.append(")");
            sb.append(this.visit(type.returnType(), Mode.USAGE));
        }
        return sb.toString();
    }

    @Override
    public String visitWildcardType(WildType type, Mode context) {
        if (type instanceof WildType.Upper wtu) {
            if ((wtu.upperBounds().size() == 1 && type.typeSystem().constants().object().equals(wtu.upperBounds().iterator().next())) ||
                    wtu.upperBounds().isEmpty()) {
                return "*";
            }
            return "+" + this.visit(wtu.upperBounds().iterator().next(), Mode.USAGE);
        } else if (type instanceof WildType.Lower wtl) {
            return "-" + this.visit(wtl.lowerBounds().iterator().next(), Mode.USAGE);
        }
        return null;
    }

    @Override
    public String visitVarType(VarType type, Mode context) {
        if (context == Mode.DECLARATION) {
            return type.name() + ":" +
                    type.upperBounds().stream().map(t -> visit(t, Mode.USAGE)).collect(Collectors.joining(":"));
        } else {
            return "T" + type.name() + ";";
        }
    }

    @Override
    public String visitMetaVarType(MetaVarType type, Mode context) {
        return "T" + type.name() + ";";
    }

    @Override
    public String visitArrayType(ArrayType type, Mode context) {
        return "[" + visit(type, context);
    }

    @Override
    public String visitIntersectionType(IntersectionType type, Mode context) {
        return type.children().stream().map(t -> this.visit(t, context)).collect(Collectors.joining(":"));
    }

    @Override
    public String visitPrimitiveType(PrimitiveType type, Mode context) {
        return type.descriptor();
    }

    @Override
    public String visitNoneType(NoneType type, Mode context) {
        return "V";
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
