package honeyroasted.jype.system;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.List;
import java.util.Map;

public interface TypeConstants {
    List<PrimitiveType> allPrimitives();

    List<ClassReference> allBoxes();

    Map<String, PrimitiveType> primitivesByName();

    Map<PrimitiveType, ClassReference> boxByPrimitive();

    Map<ClassReference, PrimitiveType> primitiveByBox();

    ClassReference object();

    ClassReference cloneable();

    ClassReference serializable();

    ClassReference runtimeException();

    NoneType voidType();

    NoneType nullType();

    NoneType noneType();

    ClassReference voidBox();

    PrimitiveType booleanType();

    PrimitiveType byteType();

    PrimitiveType shortType();

    PrimitiveType charType();

    PrimitiveType intType();

    PrimitiveType longType();

    PrimitiveType floatType();

    PrimitiveType doubleType();

    ClassReference booleanBox();

    ClassReference byteBox();

    ClassReference shortBox();

    ClassReference charBox();

    ClassReference intBox();

    ClassReference longBox();

    ClassReference floatBox();

    ClassReference doubleBox();
}
