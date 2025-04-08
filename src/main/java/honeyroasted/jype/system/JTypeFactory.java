package honeyroasted.jype.system;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

public interface JTypeFactory {
    JArrayType newArrayType();

    JClassReference newClassReference();

    JIntersectionType newIntersectionType();

    JMetaVarType newMetaVarType(String name);

    JMetaVarType newMetaVarType(int identity, String name);

    JMethodReference newMethodReference();

    JNoneType newNoneType(String name);

    JParameterizedClassType newParameterizedClassType();

    JParameterizedMethodType newParameterizedMethodType();

    JPrimitiveType newPrimitiveType(JClassNamespace namespace, JClassNamespace box, String descriptor);

    JVarType newVarType();

    JWildType.Lower newLowerWildType();

    JWildType.Upper newUpperWildType();
}
