package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.stream.Collectors;

public class JToSourceTypeVisitor implements JTypeVisitor<String, JToSourceTypeVisitor.Mode> {

    @Override
    public String visit(JType type) {
        return this.visit(type, new Mode(true, true, true));
    }

    @Override
    public String visitClassType(JClassType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context.usage()) {
            if (type.hasRelevantOuterType()) {
                sb.append(this.visit(type.outerType(), context)).append(type.namespace().name().value());
            } else {
                sb.append(context.qualifiedNames() ? type.namespace().name() : type.namespace().name().simpleName());
            }

            if (type.hasTypeArguments()) {
                sb.append("<");
                sb.append(type.typeArguments().stream().map(t -> this.visit(t, context)).collect(Collectors.joining(", ")));
                sb.append(">");
            }
        } else {
            sb.append(type.namespace().name().value());
            if (type.hasTypeParameters()) {
                sb.append("<");
                sb.append(type.typeParameters().stream().map(t -> this.visit(t, context)).collect(Collectors.joining(", ")));
                sb.append(">");
            }

            if (type.superClass() != null && !type.superClass().equals(type.typeSystem().constants().object())) {
                sb.append(" extends ").append(this.visit(type.superClass(), context.toUsage()));
            }

            if (!type.interfaces().isEmpty()) {
                sb.append(" implements ");
                sb.append(type.interfaces().stream().map(t -> this.visit(t, context.toUsage())).collect(Collectors.joining(", ")));
            }
        }
        return sb.toString();
    }

    @Override
    public String visitPrimitiveType(JPrimitiveType type, Mode context) {
        return type.name();
    }

    @Override
    public String visitWildcardType(JWildType type, Mode context) {
        if (type instanceof JWildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
            }
        } else if (type instanceof JWildType.Lower) {
            return "? super " + type.lowerBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
        }
        return "?";
    }

    @Override
    public String visitArrayType(JArrayType type, Mode context) {
        return this.visit(type.component(), context.toUsage()) + "[]";
    }

    @Override
    public String visitIntersectionType(JIntersectionType type, Mode context) {
        return type.children().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
    }

    @Override
    public String visitMethodType(JMethodType type, Mode context) {
        StringBuilder sb = new StringBuilder();
        if (context.usage()) {
            if (type.hasTypeArguments()) {
                sb.append("<");
                sb.append(type.typeArguments().stream().map(t -> this.visit(t, context)).collect(Collectors.joining(", ")));
                sb.append(">");
            }

            sb.append(type.location().name()).append("(");
            for (int i = 0; i < type.parameters().size(); i++) {
                sb.append(this.visit(type.parameters().get(i), context));
                if (context.insertParameterNames()) {
                    sb.append(" p").append(i + 1);
                }

                if (i < type.parameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        } else {
            if (type.hasTypeParameters()) {
                sb.append("<");
                sb.append(type.typeParameters().stream().map(t -> this.visit(t, context)).collect(Collectors.joining(", ")));
                sb.append("> ");
            }

            sb.append(this.visit(type.returnType(), context.toUsage()))
                    .append(" ").append(type.location().name())
                    .append("(");
            for (int i = 0; i < type.parameters().size(); i++) {
                sb.append(this.visit(type.parameters().get(i), context.toUsage()));
                if (context.insertParameterNames()) {
                    sb.append(" p").append(i + 1);
                }

                if (i < type.parameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");

            if (!type.exceptionTypes().isEmpty()) {
                sb.append(" throws ");
                sb.append(type.exceptionTypes().stream().map(t -> this.visit(t, context.toUsage())).collect(Collectors.joining(", ")));
            }

        }
        return sb.toString();
    }

    @Override
    public String visitVarType(JVarType type, Mode context) {
        return context.usage() || type.upperBounds().isEmpty() ? type.name() :
                type.name() + " extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
    }

    @Override
    public String visitMetaVarType(JMetaVarType type, Mode context) {
        return context.usage() || type.upperBounds().isEmpty() ? type.name() :
                type.name() + " extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));

    }

    @Override
    public String visitNoneType(JNoneType type, Mode context) {
        return "@" + type.name();
    }

    @Override
    public String visitFieldType(JFieldReference type, Mode context) {
        return visit(type.type(), context.toUsage()) + " " + type.location().name();
    }

    public record Mode(boolean usage, boolean qualifiedNames, boolean insertParameterNames) {
        public Mode toUsage() {
            return new Mode(true, qualifiedNames, insertParameterNames);
        }

        public Mode toDeclaration() {
            return new Mode(false, qualifiedNames, insertParameterNames);
        }
    }

}
