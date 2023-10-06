package honeyroasted.jype.system;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TypeConstants {
    private final ClassReference object;
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
    private final ClassReference booleanBox;
    private final ClassReference byteBox;
    private final ClassReference shortBox;
    private final ClassReference charBox;
    private final ClassReference intBox;
    private final ClassReference longBox;
    private final ClassReference floatBox;
    private final ClassReference doubleBox;

    public TypeConstants(ClassReference object,
                         NoneType voidType, NoneType nullType, NoneType noneType, ClassReference voidBox,
                         PrimitiveType booleanType, PrimitiveType byteType, PrimitiveType shortType,
                         PrimitiveType charType, PrimitiveType intType, PrimitiveType longType,
                         PrimitiveType floatType, PrimitiveType doubleType,
                         ClassReference booleanBox, ClassReference byteBox, ClassReference shortBox,
                         ClassReference charBox, ClassReference intBox, ClassReference longBox,
                         ClassReference floatBox, ClassReference doubleBox) {
        this.object = object;
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
        this.booleanBox = booleanBox;
        this.byteBox = byteBox;
        this.shortBox = shortBox;
        this.charBox = charBox;
        this.intBox = intBox;
        this.longBox = longBox;
        this.floatBox = floatBox;
        this.doubleBox = doubleBox;

        this.allPrimitives = List.of(booleanType, byteType, shortType, charType, intType, longType, floatType, doubleType);
        this.allBoxes = List.of(booleanBox, byteBox, shortBox, charBox, intBox, longBox, floatBox, doubleBox);

        for (int i = 0; i < allPrimitives.size(); i++) {
            boxByPrimitive.put(allPrimitives.get(i), allBoxes.get(i));
            primitiveByBox.put(allBoxes.get(i), allPrimitives.get(i));
            primitivesByName.put(allPrimitives.get(i).name(), allPrimitives.get(i));
        }
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
        return booleanBox;
    }

    public ClassReference byteBox() {
        return byteBox;
    }

    public ClassReference shortBox() {
        return shortBox;
    }

    public ClassReference charBox() {
        return charBox;
    }

    public ClassReference intBox() {
        return intBox;
    }

    public ClassReference longBox() {
        return longBox;
    }

    public ClassReference floatBox() {
        return floatBox;
    }

    public ClassReference doubleBox() {
        return doubleBox;
    }
}
