package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.ArrayTypeImpl;
import honeyroasted.jype.type.impl.MethodReferenceImpl;
import honeyroasted.jype.type.impl.ParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedMethodTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;

import java.util.List;
import java.util.Set;

public interface StructuralMappingVisitor<P> extends MappingVisitor<P> {
    @Override
    default Type visitClassType(ClassType type, P context) {
        if (type instanceof ParameterizedClassType pType) {
            List<Type> args = pType.typeArguments();
            List<Type> newArgs = visit(args, context);

            Type newOuter = visit(pType.outerType(), context);

            if (!args.equals(newArgs) || !newOuter.equals(pType.outerType())) {
                ParameterizedClassType newType = new ParameterizedClassTypeImpl(pType.typeSystem());
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
                WildType.Upper newType = new WildTypeUpperImpl(type.typeSystem());
                newType.setUpperBounds(newUpper);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof WildType.Lower lType) {
            Set<Type> newLower = visit(lType.lowerBounds(), context);
            if (!newLower.equals(lType.lowerBounds())) {
                WildType.Lower newType = new WildTypeLowerImpl(type.typeSystem());
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
            ArrayType newType = new ArrayTypeImpl(type.typeSystem());
            newType.setComponent(newComponent);
            newType.setUnmodifiable(true);
            return newType;
        }
        return type;
    }

    @Override
    default Type visitMethodType(MethodType type, P context) {
        if (type instanceof MethodReference rType) {
            List<Type> newExcept = visit(rType.exceptionTypes(), context);
            List<Type> newParams = visit(rType.parameters(), context);
            Type newRet = visit(type.returnType(), context);
            if (!newParams.equals(rType.parameters()) || !newRet.equals(type.returnType()) || !newExcept.equals(type.exceptionTypes())) {
                MethodReference newType = new MethodReferenceImpl(type.typeSystem());
                newType.setLocation(type.location());
                newType.setExceptionTypes(newExcept);
                newType.setTypeParameters(type.typeParameters());
                newType.setParameters(newParams);
                newType.setReturnType(newRet);
                newType.setUnmodifiable(true);
                return newType;
            }
        } else if (type instanceof ParameterizedMethodTypeImpl pType) {
            Type newRef = visit(pType.methodReference(), context);
            List<Type> newTypeArgs = visit(pType.typeArguments(), context);
            if (!newRef.equals(pType.methodReference()) || !newTypeArgs.equals(pType.typeArguments())) {
                ParameterizedMethodType newType = new ParameterizedMethodTypeImpl(type.typeSystem());
                newType.setMethodReference(newRef instanceof MethodReference ref ? ref : pType.methodReference());
                newType.setTypeArguments(newTypeArgs);
                newType.setUnmodifiable(true);
                return newType;
            }
        }
        return type;
    }
}