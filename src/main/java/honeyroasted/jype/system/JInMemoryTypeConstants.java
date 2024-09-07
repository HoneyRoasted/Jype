package honeyroasted.jype.system;

import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JInMemoryTypeConstants implements JTypeConstants {
    private final JClassReference object;
    private JClassReference cloneable;
    private JClassReference serializable;
    private JClassReference runtimeException;
    private final JNoneType voidType;
    private final JNoneType nullType;
    private final JNoneType noneType;
    private final JClassReference voidBox;
    private final JPrimitiveType booleanType;
    private final JPrimitiveType byteType;
    private final JPrimitiveType shortType;
    private final JPrimitiveType charType;
    private final JPrimitiveType intType;
    private final JPrimitiveType longType;
    private final JPrimitiveType floatType;
    private final JPrimitiveType doubleType;

    public JInMemoryTypeConstants(JClassReference object, JClassReference cloneable, JClassReference serializable,
                                 JClassReference runtimeException,
                                 JNoneType voidType, JNoneType nullType, JNoneType noneType, JClassReference voidBox,
                                 JPrimitiveType booleanType, JPrimitiveType byteType, JPrimitiveType shortType,
                                 JPrimitiveType charType, JPrimitiveType intType, JPrimitiveType longType,
                                 JPrimitiveType floatType, JPrimitiveType doubleType) {
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
        List<JClassReference> allBoxes = new ArrayList<>();

        for (JPrimitiveType type : this.allPrimitives) {
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

    private final List<JPrimitiveType> allPrimitives;
    private final List<JClassReference> allBoxes;

    private Map<String, JPrimitiveType> primitivesByName = new LinkedHashMap<>();
    private Map<JPrimitiveType, JClassReference> boxByPrimitive = new LinkedHashMap<>();
    private Map<JClassReference, JPrimitiveType> primitiveByBox = new LinkedHashMap<>();

    @Override
    public List<JPrimitiveType> allPrimitives() {
        return allPrimitives;
    }

    @Override
    public List<JClassReference> allBoxes() {
        return allBoxes;
    }

    @Override
    public Map<String, JPrimitiveType> primitivesByName() {
        return this.primitivesByName;
    }

    @Override
    public Map<JPrimitiveType, JClassReference> boxByPrimitive() {
        return this.boxByPrimitive;
    }

    @Override
    public Map<JClassReference, JPrimitiveType> primitiveByBox() {
        return this.primitiveByBox;
    }

    @Override
    public JClassReference object() {
        return object;
    }

    @Override
    public JClassReference cloneable() {
        return this.cloneable;
    }

    @Override
    public JClassReference serializable() {
        return this.serializable;
    }

    @Override
    public JClassReference runtimeException() {
        return this.runtimeException;
    }

    @Override
    public JNoneType voidType() {
        return voidType;
    }

    @Override
    public JNoneType nullType() {
        return nullType;
    }

    @Override
    public JNoneType noneType() {
        return noneType;
    }

    @Override
    public JClassReference voidBox() {
        return voidBox;
    }

    @Override
    public JPrimitiveType booleanType() {
        return booleanType;
    }

    @Override
    public JPrimitiveType byteType() {
        return byteType;
    }

    @Override
    public JPrimitiveType shortType() {
        return shortType;
    }

    @Override
    public JPrimitiveType charType() {
        return charType;
    }

    @Override
    public JPrimitiveType intType() {
        return intType;
    }

    @Override
    public JPrimitiveType longType() {
        return longType;
    }

    @Override
    public JPrimitiveType floatType() {
        return floatType;
    }

    @Override
    public JPrimitiveType doubleType() {
        return doubleType;
    }

    @Override
    public JClassReference booleanBox() {
        return booleanType.box();
    }

    @Override
    public JClassReference byteBox() {
        return byteType.box();
    }

    @Override
    public JClassReference shortBox() {
        return shortType.box();
    }

    @Override
    public JClassReference charBox() {
        return charType.box();
    }

    @Override
    public JClassReference intBox() {
        return intType.box();
    }

    @Override
    public JClassReference longBox() {
        return longType.box();
    }

    @Override
    public JClassReference floatBox() {
        return floatType.box();
    }

    @Override
    public JClassReference doubleBox() {
        return doubleType.box();
    }
}
