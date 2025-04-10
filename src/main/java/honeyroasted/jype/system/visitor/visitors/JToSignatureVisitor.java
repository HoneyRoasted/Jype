package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.util.Collections;

public class JToSignatureVisitor implements JTypeVisitor<JSignature, JToSignatureVisitor.Mode> {
    @Override
    public JSignature visit(JType type) {
        return this.visit(type, Mode.USAGE);
    }

    @Override
    public JSignature visitPrimitiveType(JPrimitiveType type, Mode context) {
        return new JSignature.Type(type.descriptor());
    }

    @Override
    public JSignature visitNoneType(JNoneType type, Mode context) {
        return new JSignature.Type(JDescriptor.Primitive.VOID);
    }

    @Override
    public JSignature visitMetaVarType(JMetaVarType type, Mode context) {
        return new JSignature.TypeVar(type.name());
    }

    @Override
    public JSignature visitArrayType(JArrayType type, Mode context) {
        return new JSignature.Array((JSignature.InformalType) visit(type.component(), Mode.USAGE));
    }

    @Override
    public JSignature visitIntersectionType(JIntersectionType type, Mode context) {
        return new JSignature.IntersectionType(type.children().stream().map(t -> (JSignature.InformalType) visit(t, Mode.USAGE)).toList());
    }

    @Override
    public JSignature visitVarType(JVarType type, Mode context) {
        if (context.isDeclaration()) {
            if (type.upperBounds().isEmpty()) {
                return new JSignature.TypeVarDeclaration(type.name(), null, Collections.emptyList());
            } else {
                JType first = type.upperBounds().iterator().next();
                if (first instanceof JClassType jct && jct.hasModifier(AccessFlag.INTERFACE)) {
                    return new JSignature.TypeVarDeclaration(type.name(), null,
                            type.upperBounds().stream().map(j -> (JSignature.InformalType) visit(j, Mode.USAGE)).toList());
                } else {
                    return new JSignature.TypeVarDeclaration(type.name(), (JSignature.InformalType) visit(first, Mode.USAGE),
                            type.upperBounds().stream().skip(1).map(j -> (JSignature.InformalType) visit(j, Mode.USAGE)).toList());
                }
            }
        } else {
            return new JSignature.TypeVar(type.name());
        }
    }

    @Override
    public JSignature visitWildcardType(JWildType type, Mode context) {
        if (type instanceof JWildType.Upper wtu) {
            if ((wtu.upperBounds().size() == 1 && type.typeSystem().constants().object().equals(wtu.upperBounds().iterator().next())) ||
                    wtu.upperBounds().isEmpty()) {
                return new JSignature.WildType(null, null);
            } else {
                return new JSignature.WildType((JSignature.InformalType) visit(wtu.upperBounds().iterator().next(), Mode.USAGE), null);
            }

        } else if (type instanceof JWildType.Lower wtl) {
            return new JSignature.WildType(null, (JSignature.InformalType) visit(wtl.lowerBounds().iterator().next(), Mode.USAGE));
        }
        return new JSignature.WildType(null, null);
    }

    @Override
    public JSignature visitMethodType(JMethodType type, Mode context) {
        return new JSignature.MethodDeclaration(
                type.typeParameters().stream().map(jt -> (JSignature.TypeVarDeclaration) visit(jt, Mode.DECLARATION)).toList(),
                type.parameters().stream().map(jt -> (JSignature.InformalType) visit(jt, Mode.USAGE)).toList(),
                (JSignature.InformalType) visit(type.returnType(), Mode.USAGE),
                type.exceptionTypes().stream().map(jt -> (JSignature.InformalType) visit(jt, Mode.USAGE)).toList()
        );
    }

    @Override
    public JSignature visitClassType(JClassType type, Mode context) {
        if (context.isDeclaration()) {
            return new JSignature.ClassDeclaration(
                    type.typeParameters().stream().map(jt -> (JSignature.TypeVarDeclaration) visit(jt, Mode.DECLARATION)).toList(),
                    (JSignature.InformalType) visit(type, Mode.USAGE),
                    type.interfaces().stream().map(jt -> (JSignature.InformalType) visit(jt, Mode.USAGE)).toList()
            );
        } else {
            JClassLocation loc = type.namespace().location();

            JSignature.Type typeSig = new JSignature.Type(new JDescriptor.Class(loc.getPackage().toArray(), loc.value()));
            boolean needsOuter =  (type.hasRelevantOuterType() && type.outerType().hasAnyTypeArguments());
            if (type.hasTypeArguments() || needsOuter) {
                if (needsOuter) {
                    JSignature outer = visit(type.outerType(), Mode.USAGE);

                    return new JSignature.Parameterized(
                            outer instanceof JSignature.Type jst ? jst.asParameterized() : (JSignature.Parameterized) outer,
                            new JSignature.Type(new JDescriptor.Class(loc.getPackage().toArray(), type.namespace().name().value())),
                            type.typeArguments().stream().map(jt -> (JSignature.InformalType) visit(jt, Mode.USAGE)).toList()
                    );
                } else {
                    return new JSignature.Parameterized(
                            typeSig,
                            type.typeArguments().stream().map(jt -> (JSignature.InformalType) visit(jt, Mode.USAGE)).toList()
                    );
                }
            } else {
                return typeSig;
            }
        }
    }

    public enum Mode {
        DECLARATION {
            @Override
            boolean isUsage() {
                return false;
            }
        },
        USAGE {
            @Override
            boolean isUsage() {
                return true;
            }
        };

        boolean isDeclaration() {
            return !isUsage();
        }

        abstract boolean isUsage();
    }
}
