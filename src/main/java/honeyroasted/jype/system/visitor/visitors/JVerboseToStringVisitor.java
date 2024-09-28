package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.Set;
import java.util.stream.Collectors;

public class JVerboseToStringVisitor implements JToStringVisitor {
    private boolean includeVarBounds;
    private boolean includeClassSupertypes;
    private boolean includeTypeParameters;
    private boolean includeTypeArguments;
    private boolean includeMethodExceptions;

    private String startWrap;
    private String endWrap;

    public JVerboseToStringVisitor(boolean includeVarBounds, boolean includeClassSupertypes, boolean includeTypeParameters, boolean includeTypeArguments, boolean includeMethodExceptions, String startWrap, String endWrap) {
        this.includeVarBounds = includeVarBounds;
        this.includeClassSupertypes = includeClassSupertypes;
        this.includeTypeParameters = includeTypeParameters;
        this.includeTypeArguments = includeTypeArguments;
        this.includeMethodExceptions = includeMethodExceptions;
        this.startWrap = startWrap;
        this.endWrap = endWrap;
    }

    @Override
    public String classToString(JClassType type, Set<JType> context) {
        StringBuilder sb = new StringBuilder();
        if (type.hasRelevantOuterType()) {
            sb.append(startWrap).append(visit(type.outerType(), context))
                    .append(endWrap).append(".").append(type.namespace().name().value());
        } else {
            sb.append(type.namespace());
        }

        if (type instanceof JClassReference cr) {
            if (this.includeTypeParameters && cr.hasTypeParameters()) {
                sb.append("<");
                for (int i = 0; i < cr.typeParameters().size(); i++) {
                    JVarType vt = cr.typeParameters().get(i);
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
                        JClassType ct = type.interfaces().get(i);
                        sb.append(startWrap)
                                .append(visit(ct, context))
                                .append(endWrap);
                        if (i < type.interfaces().size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
            }
        } else if (type instanceof JParameterizedClassType pct) {
            if (this.includeTypeArguments && pct.hasTypeArguments()) {
                sb.append("<");
                for (int i = 0; i < pct.typeArguments().size(); i++) {
                    JType arg = pct.typeArguments().get(i);
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
                        JClassType ct = type.interfaces().get(i);
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
    public String wildcardToString(JWildType type, Set<JType> context) {
        if (type instanceof JWildType.Upper wtu) {
            if (wtu.hasDefaultBounds()) {
                return "?";
            } else {
                return "? extends " + type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(", "));
            }
        } else if (type instanceof JWildType.Lower) {
            return "? super " + type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(", "));
        }
        return "?";
    }

    @Override
    public String arrayToString(JArrayType type, Set<JType> context) {
        return visit(type.component(), context) + "[]";
    }

    @Override
    public String intersectionToString(JIntersectionType type, Set<JType> context) {
        return type.children().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & "));
    }

    @Override
    public String methodToString(JMethodType type, Set<JType> context) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.location().simpleName());
        if (type instanceof JMethodReference mr && this.includeTypeParameters && mr.hasTypeParameters()) {
            sb.append("<");
            for (int i = 0; i < mr.typeParameters().size(); i++) {
                sb.append(visit(mr.typeParameters().get(i), context));
                if (i < mr.typeParameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        } else if (type instanceof JParameterizedMethodType pmt && this.includeTypeArguments && pmt.hasTypeArguments()) {
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

    public String varToString(JVarType type, Set<JType> context) {
        return type.location() + (!this.includeVarBounds || type.hasDefaultBounds() ? "" : " extends " +
                type.upperBounds().stream().map(t -> visit(t, context)).collect(Collectors.joining(" & ")));
    }

    public String primToString(JPrimitiveType type, Set<JType> context) {
        return type.name();
    }

    public String metaVarToString(JMetaVarType type, Set<JType> context) {
        return "#" + type.name() + ":" + Integer.toString(type.identity(), 16);
    }

    public String noneToString(JNoneType type, Set<JType> context) {
        return "@" + type.name();
    }

}
