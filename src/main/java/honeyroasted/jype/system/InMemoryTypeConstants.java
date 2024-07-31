package honeyroasted.jype.system;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryTypeConstants implements TypeConstants {
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

    public InMemoryTypeConstants(ClassReference object, ClassReference cloneable, ClassReference serializable,
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

    @Override
    public List<PrimitiveType> allPrimitives() {
        return allPrimitives;
    }

    @Override
    public List<ClassReference> allBoxes() {
        return allBoxes;
    }

    @Override
    public Map<String, PrimitiveType> primitivesByName() {
        return this.primitivesByName;
    }

    @Override
    public Map<PrimitiveType, ClassReference> boxByPrimitive() {
        return this.boxByPrimitive;
    }

    @Override
    public Map<ClassReference, PrimitiveType> primitiveByBox() {
        return this.primitiveByBox;
    }

    @Override
    public ClassReference object() {
        return object;
    }

    @Override
    public ClassReference cloneable() {
        return this.cloneable;
    }

    @Override
    public ClassReference serializable() {
        return this.serializable;
    }

    @Override
    public ClassReference runtimeException() {
        return this.runtimeException;
    }

    @Override
    public NoneType voidType() {
        return voidType;
    }

    @Override
    public NoneType nullType() {
        return nullType;
    }

    @Override
    public NoneType noneType() {
        return noneType;
    }

    @Override
    public ClassReference voidBox() {
        return voidBox;
    }

    @Override
    public PrimitiveType booleanType() {
        return booleanType;
    }

    @Override
    public PrimitiveType byteType() {
        return byteType;
    }

    @Override
    public PrimitiveType shortType() {
        return shortType;
    }

    @Override
    public PrimitiveType charType() {
        return charType;
    }

    @Override
    public PrimitiveType intType() {
        return intType;
    }

    @Override
    public PrimitiveType longType() {
        return longType;
    }

    @Override
    public PrimitiveType floatType() {
        return floatType;
    }

    @Override
    public PrimitiveType doubleType() {
        return doubleType;
    }

    @Override
    public ClassReference booleanBox() {
        return booleanType.box();
    }

    @Override
    public ClassReference byteBox() {
        return byteType.box();
    }

    @Override
    public ClassReference shortBox() {
        return shortType.box();
    }

    @Override
    public ClassReference charBox() {
        return charType.box();
    }

    @Override
    public ClassReference intBox() {
        return intType.box();
    }

    @Override
    public ClassReference longBox() {
        return longType.box();
    }

    @Override
    public ClassReference floatBox() {
        return floatType.box();
    }

    @Override
    public ClassReference doubleBox() {
        return doubleType.box();
    }
}
