package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;
import honeyroasted.jype.type.impl.JParameterizedMethodTypeImpl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface JStructuralTypeMappingVisitor<P> extends JTypeMappingVisitor<P> {
    @Override
    default JType visitClassType(JClassType type, P context) {
        if (type instanceof JParameterizedClassType pType) {
            List<JArgumentType> args = pType.typeArguments();
            List<JArgumentType> newArgs = (List) visit(args, context);

            JType newOuter = visit(pType.outerType(), context);

            if (!args.equals(newArgs) || !newOuter.equals(pType.outerType())) {
                JParameterizedClassType newType = pType.typeSystem().typeFactory().newParameterizedClassType();
                newType.setClassReference(pType.classReference());
                newType.setOuterType(newOuter instanceof JClassType ct ? ct : pType.outerType());
                newType.setTypeArguments(newArgs);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }

    @Override
    default JType visitWildcardType(JWildType type, P context) {
        if (type instanceof JWildType.Upper uType) {
            Set<JType> newUpper = visit(uType.upperBounds(), context);
            if (!newUpper.equals(uType.upperBounds())) {
                JWildType.Upper newType = type.typeSystem().typeFactory().newUpperWildType();
                newType.setUpperBounds(newUpper);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof JWildType.Lower lType) {
            Set<JType> newLower = visit(lType.lowerBounds(), context);
            if (!newLower.equals(lType.lowerBounds())) {
                JWildType.Lower newType = type.typeSystem().typeFactory().newLowerWildType();
                newType.setLowerBounds(newLower);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }

    @Override
    default JType visitArrayType(JArrayType type, P context) {
        JType newComponent = visit(type.component(), context);
        if (!newComponent.equals(type.component())) {
            JArrayType newType = type.typeSystem().typeFactory().newArrayType();
            newType.setComponent(newComponent);
            newType.setUnmodifiable(true);
            return newType;
        }
        return type;
    }

    @Override
    default JType visitIntersectionType(JIntersectionType type, P context) {
        Set<JType> newChildren = new LinkedHashSet<>();
        type.children().forEach(t -> newChildren.add(this.visit(t, context)));

        if (!newChildren.equals(type.children())) {
            return JIntersectionType.of(newChildren, type.typeSystem());
        }
        return type;
    }

    @Override
    default JType visitMethodType(JMethodType type, P context) {
        if (type instanceof JMethodReference rType) {
            List<JType> newExcept = visit(rType.exceptionTypes(), context);
            List<JType> newParams = visit(rType.parameters(), context);
            JType newRet = visit(type.returnType(), context);
            JType newOuter = visit(type.outerClass(), context);
            if (!newParams.equals(rType.parameters()) || !newRet.equals(type.returnType()) || !newExcept.equals(type.exceptionTypes()) || !newOuter.equals(type.outerClass())) {
                JMethodReference newType = type.typeSystem().typeFactory().newMethodReference();
                newType.setLocation(type.location());
                newType.setOuterClass(newOuter instanceof JClassReference cr ? cr : rType.outerClass());
                newType.setExceptionTypes(newExcept);
                newType.setTypeParameters(type.typeParameters());
                newType.setParameters(newParams);
                newType.setReturnType(newRet);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof JParameterizedMethodTypeImpl pType) {
            JType newRef = visit(pType.methodReference(), context);
            JType newOuter = visit(pType.outerType(), context);
            List<JArgumentType> newTypeArgs = (List) visit(pType.typeArguments(), context);
            if (!newRef.equals(pType.methodReference()) || !newTypeArgs.equals(pType.typeArguments()) || !newOuter.equals(pType.outerType())) {
                JParameterizedMethodType newType = type.typeSystem().typeFactory().newParameterizedMethodType();
                newType.setMethodReference(newRef instanceof JMethodReference ref ? ref : pType.methodReference());
                newType.setOuterType(newOuter instanceof JClassType ct ? ct : pType.outerType());
                newType.setTypeArguments(newTypeArgs);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }
}