package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.ParameterizedMethodTypeImpl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface StructuralTypeMappingVisitor<P> extends TypeMappingVisitor<P> {
    @Override
    default Type visitClassType(ClassType type, P context) {
        if (type instanceof ParameterizedClassType pType) {
            List<ArgumentType> args = pType.typeArguments();
            List<ArgumentType> newArgs = (List) visit(args, context);

            Type newOuter = visit(pType.outerType(), context);

            if (!args.equals(newArgs) || !newOuter.equals(pType.outerType())) {
                ParameterizedClassType newType = pType.typeSystem().typeFactory().newParameterizedClassType();
                newType.setClassReference(pType.classReference());
                newType.setOuterType(newOuter instanceof ClassType ct ? ct : pType.outerType());
                newType.setTypeArguments(newArgs);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }

    @Override
    default Type visitWildcardType(WildType type, P context) {
        if (type instanceof WildType.Upper uType) {
            Set<Type> newUpper = visit(uType.upperBounds(), context);
            if (!newUpper.equals(uType.upperBounds())) {
                WildType.Upper newType = type.typeSystem().typeFactory().newUpperWildType();
                newType.setUpperBounds(newUpper);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof WildType.Lower lType) {
            Set<Type> newLower = visit(lType.lowerBounds(), context);
            if (!newLower.equals(lType.lowerBounds())) {
                WildType.Lower newType = type.typeSystem().typeFactory().newLowerWildType();
                newType.setLowerBounds(newLower);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }

    @Override
    default Type visitArrayType(ArrayType type, P context) {
        Type newComponent = visit(type.component(), context);
        if (!newComponent.equals(type.component())) {
            ArrayType newType = type.typeSystem().typeFactory().newArrayType();
            newType.setComponent(newComponent);
            newType.setUnmodifiable(true);
            return newType;
        }
        return type;
    }

    @Override
    default Type visitIntersectionType(IntersectionType type, P context) {
        Set<Type> newChildren = new LinkedHashSet<>();
        type.children().forEach(t -> newChildren.add(this.visit(t, context)));

        if (!newChildren.equals(type.children())) {
            return IntersectionType.of(newChildren, type.typeSystem());
        }
        return type;
    }

    @Override
    default Type visitMethodType(MethodType type, P context) {
        if (type instanceof MethodReference rType) {
            List<Type> newExcept = visit(rType.exceptionTypes(), context);
            List<Type> newParams = visit(rType.parameters(), context);
            Type newRet = visit(type.returnType(), context);
            Type newOuter = visit(type.outerClass(), context);
            if (!newParams.equals(rType.parameters()) || !newRet.equals(type.returnType()) || !newExcept.equals(type.exceptionTypes()) || !newOuter.equals(type.outerClass())) {
                MethodReference newType = type.typeSystem().typeFactory().newMethodReference();
                newType.setLocation(type.location());
                newType.setOuterClass(newOuter instanceof ClassReference cr ? cr : rType.outerClass());
                newType.setExceptionTypes(newExcept);
                newType.setTypeParameters(type.typeParameters());
                newType.setParameters(newParams);
                newType.setReturnType(newRet);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof ParameterizedMethodTypeImpl pType) {
            Type newRef = visit(pType.methodReference(), context);
            Type newOuter = visit(pType.outerType(), context);
            List<ArgumentType> newTypeArgs = (List) visit(pType.typeArguments(), context);
            if (!newRef.equals(pType.methodReference()) || !newTypeArgs.equals(pType.typeArguments()) || !newOuter.equals(pType.outerType())) {
                ParameterizedMethodType newType = type.typeSystem().typeFactory().newParameterizedMethodType();
                newType.setMethodReference(newRef instanceof MethodReference ref ? ref : pType.methodReference());
                newType.setOuterType(newOuter instanceof ClassType ct ? ct : pType.outerType());
                newType.setTypeArguments(newTypeArgs);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }
}