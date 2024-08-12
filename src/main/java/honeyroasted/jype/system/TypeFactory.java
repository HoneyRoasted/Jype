package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

public interface TypeFactory {
    ArrayType newArrayType();

    ClassReference newClassReference();

    IntersectionType newIntersectionType();

    MetaVarType newMetaVarType(String name);

    MetaVarType newMetaVarType(int identity, String name);

    MethodReference newMethodReference();

    NoneType newNoneType(String name);

    ParameterizedClassType newParameterizedClassType();

    ParameterizedMethodType newParameterizedMethodType();

    PrimitiveType newPrimitiveType(ClassNamespace namespace, ClassReference box, String descriptor);

    VarType newVarType();

    WildType.Lower newLowerWildType();

    WildType.Upper newUpperWildType();
}
