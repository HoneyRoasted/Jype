package honeyroasted.jype.system;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TypeConstants {
    private final ClassReference object;
    private ClassReference cloneable;
    private ClassReference serializable;
    private ClassReference runtimeException;
    private final NoneType voidType;
    private final NoneType nullType;
    private final NoneType noneType;
    private final ClassReference voidBox;
    private final PrimitiveType booleanType;
    private final PrimitiveType byteType;
    private final PrimitiveType shortType;
    private final PrimitiveType charType;
    private final PrimitiveType intType;
    private final PrimitiveType longType;
    private final PrimitiveType floatType;
    private final PrimitiveType doubleType;

    public TypeConstants(ClassReference object, ClassReference cloneable, ClassReference serializable,
                         ClassReference runtimeException,
                         NoneType voidType, NoneType nullType, NoneType noneType, ClassReference voidBox,
                         PrimitiveType booleanType, PrimitiveType byteType, PrimitiveType shortType,
                         PrimitiveType charType, PrimitiveType intType, PrimitiveType longType,
                         PrimitiveType floatType, PrimitiveType doubleType) {
        this.object = object;
        this.cloneable = cloneable;
        this.serializable = serializable;
        this.runtimeException = runtimeException;
        this.voidType = voidType;
        this.nullType = nullType;
        this.noneType = noneType;
        this.voidBox = voidBox;
        this.booleanType = booleanType;
        this.byteType = byteType;
        this.shortType = shortType;
        this.charType = charType;
        this.intType = intType;
        this.longType = longType;
        this.floatType = floatType;
        this.doubleType = doubleType;

        this.allPrimitives = List.of(booleanType, byteType, shortType, charType, intType, longType, floatType, doubleType);
        List<ClassReference> allBoxes = new ArrayList<>();

        for (PrimitiveType type : this.allPrimitives) {
            allBoxes.add(type.box());
            boxByPrimitive.put(type, type.box());
            primitiveByBox.put(type.box(), type);
            primitivesByName.put(type.name(), type);
        }

        this.allBoxes = Collections.unmodifiableList(allBoxes);
        this.boxByPrimitive = Collections.unmodifiableMap(boxByPrimitive);
        this.primitivesByName = Collections.unmodifiableMap(primitivesByName);
        this.primitiveByBox = Collections.unmodifiableMap(primitiveByBox);
    }

    private final List<PrimitiveType> allPrimitives;
    private final List<ClassReference> allBoxes;

    private Map<String, PrimitiveType> primitivesByName = new LinkedHashMap<>();
    private Map<PrimitiveType, ClassReference> boxByPrimitive = new LinkedHashMap<>();
    private Map<ClassReference, PrimitiveType> primitiveByBox = new LinkedHashMap<>();

    public List<PrimitiveType> allPrimitives() {
        return allPrimitives;
    }

    public List<ClassReference> allBoxes() {
        return allBoxes;
    }

    public Map<String, PrimitiveType> primitivesByName() {
        return this.primitivesByName;
    }

    public Map<PrimitiveType, ClassReference> boxByPrimitive() {
        return this.boxByPrimitive;
    }

    public Map<ClassReference, PrimitiveType> primitiveByBox() {
        return this.primitiveByBox;
    }

    public ClassReference object() {
        return object;
    }

    public ClassReference cloneable() {
        return this.cloneable;
    }

    public ClassReference serializable() {
        return this.serializable;
    }

    public ClassReference runtimeException() {
        return this.runtimeException;
    }

    public NoneType voidType() {
        return voidType;
    }

    public NoneType nullType() {
        return nullType;
    }

    public NoneType noneType() {
        return noneType;
    }

    public ClassReference voidBox() {
        return voidBox;
    }

    public PrimitiveType booleanType() {
        return booleanType;
    }

    public PrimitiveType byteType() {
        return byteType;
    }

    public PrimitiveType shortType() {
        return shortType;
    }

    public PrimitiveType charType() {
        return charType;
    }

    public PrimitiveType intType() {
        return intType;
    }

    public PrimitiveType longType() {
        return longType;
    }

    public PrimitiveType floatType() {
        return floatType;
    }

    public PrimitiveType doubleType() {
        return doubleType;
    }

    public ClassReference booleanBox() {
        return booleanType.box();
    }

    public ClassReference byteBox() {
        return byteType.box();
    }

    public ClassReference shortBox() {
        return shortType.box();
    }

    public ClassReference charBox() {
        return charType.box();
    }

    public ClassReference intBox() {
        return intType.box();
    }

    public ClassReference longBox() {
        return longType.box();
    }

    public ClassReference floatBox() {
        return floatType.box();
    }

    public ClassReference doubleBox() {
        return doubleType.box();
    }
}
