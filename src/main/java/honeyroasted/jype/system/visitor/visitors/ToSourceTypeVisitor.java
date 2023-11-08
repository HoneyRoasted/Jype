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

import java.util.stream.Collectors;

public class ToSourceTypeVisitor implements TypeVisitor<String, ToSourceTypeVisitor.Mode> {

    @Override
    public String visit(Type type) {
        return this.visit(type, new Mode(true, true, true));
    }

    @Override
    public String visitClassType(ClassType type, Mode context) {
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
    public String visitPrimitiveType(PrimitiveType type, Mode context) {
        return type.name();
    }

    @Override
    public String visitWildcardType(WildType type, Mode context) {
        if (type instanceof WildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
            }
        } else if (type instanceof WildType.Lower) {
            return "? super " + type.lowerBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
        }
        return "?";
    }

    @Override
    public String visitArrayType(ArrayType type, Mode context) {
        return this.visit(type.component(), context.toUsage()) + "[]";
    }

    @Override
    public String visitIntersectionType(IntersectionType type, Mode context) {
        return type.children().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
    }

    @Override
    public String visitMethodType(MethodType type, Mode context) {
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
    public String visitVarType(VarType type, Mode context) {
        return context.usage() || type.upperBounds().isEmpty() ? type.name() :
                type.name() + " extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));
    }

    @Override
    public String visitMetaVarType(MetaVarType type, Mode context) {
        return context.usage() || type.upperBounds().isEmpty() ? type.name() :
                type.name() + " extends " + type.upperBounds().stream().map(t -> visit(t, context.toUsage())).collect(Collectors.joining(" & "));

    }

    @Override
    public String visitNoneType(NoneType type, Mode context) {
        return "@" + type.name();
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
