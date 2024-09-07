package honeyroasted.jype.system;

import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;

import java.util.List;
import java.util.Map;

public interface JTypeConstants {
    List<JPrimitiveType> allPrimitives();

    List<JClassReference> allBoxes();

    Map<String, JPrimitiveType> primitivesByName();

    Map<JPrimitiveType, JClassReference> boxByPrimitive();

    Map<JClassReference, JPrimitiveType> primitiveByBox();

    JClassReference object();

    JClassReference cloneable();

    JClassReference serializable();

    JClassReference runtimeException();

    JNoneType voidType();

    JNoneType nullType();

    JNoneType noneType();

    JClassReference voidBox();

    JPrimitiveType booleanType();

    JPrimitiveType byteType();

    JPrimitiveType shortType();

    JPrimitiveType charType();

    JPrimitiveType intType();

    JPrimitiveType longType();

    JPrimitiveType floatType();

    JPrimitiveType doubleType();

    JClassReference booleanBox();

    JClassReference byteBox();

    JClassReference shortBox();

    JClassReference charBox();

    JClassReference intBox();

    JClassReference longBox();

    JClassReference floatBox();

    JClassReference doubleBox();
}
