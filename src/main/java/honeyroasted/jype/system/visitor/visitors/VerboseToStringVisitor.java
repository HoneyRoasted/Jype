package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.Set;
import java.util.stream.Collectors;

public class VerboseToStringVisitor implements ToStringVisitor {
    private boolean includeVarBounds;
    private boolean includeClassSupertypes;
    private boolean includeTypeParameters;
    private boolean includeTypeArguments;
    private boolean includeMethodExceptions;

    private String startWrap;
    private String endWrap;

    public VerboseToStringVisitor(boolean includeVarBounds, boolean includeClassSupertypes, boolean includeTypeParameters, boolean includeTypeArguments, boolean includeMethodExceptions, String startWrap, String endWrap) {
        this.includeVarBounds = includeVarBounds;
        this.includeClassSupertypes = includeClassSupertypes;
        this.includeTypeParameters = includeTypeParameters;
        this.includeTypeArguments = includeTypeArguments;
        this.includeMethodExceptions = includeMethodExceptions;
        this.startWrap = startWrap;
        this.endWrap = endWrap;
    }

    @Override
    public String classToString(ClassType type, Set<Type> context) {
        StringBuilder sb = new StringBuilder();
        if (type.hasRelevantOuterType()) {
            sb.append(startWrap).append(visit(type.outerType()))
                    .append(endWrap).append(".").append(type.namespace().name().value());
        } else {
            sb.append(type.namespace());
        }

        if (type instanceof ClassReference cr) {
            if (this.includeTypeParameters && cr.hasTypeParameters()) {
                sb.append("<");
                for (int i = 0; i < cr.typeParameters().size(); i++) {
                    VarType vt = cr.typeParameters().get(i);
                    sb.append(visit(vt, context));
                    if (i < cr.typeParameters().size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
            }

            if (this.includeClassSupertypes) {
                if (type.superClass() != null) {
                    sb.append(" extends ")
                            .append(startWrap)
                            .append(visit(type.superClass(), context))
                            .append(endWrap);
                }

                if (!type.interfaces().isEmpty()) {
                    sb.append(" implements ");
                    for (int i = 0; i < type.interfaces().size(); i++) {
                        ClassType ct = type.interfaces().get(i);
                        sb.append(startWrap)
                                .append(visit(ct, context))
                                .append(endWrap);
                        if (i < type.interfaces().size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
            }
        } else if (type instanceof ParameterizedClassType pct) {
            if (this.includeTypeArguments && pct.hasTypeArguments()) {
                sb.append("<");
                for (int i = 0; i < pct.typeArguments().size(); i++) {
                    Type arg = pct.typeArguments().get(i);
                    sb.append(visit(arg, context));
                    if (i < pct.typeArguments().size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
            }

            if (this.includeClassSupertypes) {
                if (type.superClass() != null) {
                    sb.append(" extends ")
                            .append(startWrap)
                            .append(visit(pct.directSupertype(type.superClass()), context))
                            .append(endWrap);
                }

                if (!type.interfaces().isEmpty()) {
                    sb.append(" implements ");
                    for (int i = 0; i < type.interfaces().size(); i++) {
                        ClassType ct = type.interfaces().get(i);
                        sb.append(startWrap)
                                .append(visit(pct.directSupertype(ct), context))
                                .append(endWrap);
                        if (i < type.interfaces().size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String wildcardToString(WildType type, Set<Type> context) {
        if (type instanceof WildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(", "));
            }
        } else if (type instanceof WildType.Lower) {
            return "? super " + type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(", "));
        }
        return "?";
    }

    @Override
    public String arrayToString(ArrayType type, Set<Type> context) {
        return "[" + visit(type.component(), context) + "]";
    }

    @Override
    public String intersectionToString(IntersectionType type, Set<Type> context) {
        return type.children().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & "));
    }

    @Override
    public String methodToString(MethodType type, Set<Type> context) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.location().simpleName());
        if (type instanceof MethodReference mr && this.includeTypeParameters && mr.hasTypeParameters()) {
            sb.append("<");
            for (int i = 0; i < mr.typeParameters().size(); i++) {
                sb.append(visit(mr.typeParameters().get(i), context));
                if (i < mr.typeParameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        } else if (type instanceof ParameterizedMethodType pmt && this.includeTypeArguments && pmt.hasTypeArguments()) {
            sb.append("<");
            for (int i = 0; i < pmt.typeArguments().size(); i++) {
                sb.append(visit(pmt.typeArguments().get(i), context));
                if (i < pmt.typeArguments().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }

        sb.append("(");
        for (int i = 0; i < type.parameters().size(); i++) {
            sb.append(visit(type.parameters().get(i)));
            if (i < type.parameters().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")->").append(visit(type.returnType(), context));

        if (this.includeMethodExceptions && !type.exceptionTypes().isEmpty()) {
            sb.append(" throws ");
            for (int i = 0; i < type.exceptionTypes().size(); i++) {
                sb.append(visit(type.exceptionTypes().get(i)));
                if (i < type.exceptionTypes().size() - 1) {
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }

    public String varToString(VarType type, Set<Type> context) {
        return type.location() + (!this.includeVarBounds || type.hasDefaultBounds() ? "" : " extends " +
                type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & ")));
    }

    public String primToString(PrimitiveType type, Set<Type> context) {
        return type.name();
    }

    public String metaVarToString(MetaVarType type, Set<Type> context) {
        return "%" + type.name() + "/" + Integer.toString(type.identity(), 16);
    }

    public String noneToString(NoneType type, Set<Type> context) {
        return "@" + type.name();
    }

}
