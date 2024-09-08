package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JInMemoryTypeConstants implements JTypeConstants {
    private JClassReference object;
    private JClassReference cloneable;
    private JClassReference serializable;
    private JClassReference runtimeException;
    private JNoneType voidType;
    private JNoneType nullType;
    private JNoneType noneType;
    private JClassReference voidBox;
    private JPrimitiveType booleanType;
    private JPrimitiveType byteType;
    private JPrimitiveType shortType;
    private JPrimitiveType charType;
    private JPrimitiveType intType;
    private JPrimitiveType longType;
    private JPrimitiveType floatType;
    private JPrimitiveType doubleType;


    private List<JPrimitiveType> allPrimitives;
    private List<JClassReference> allBoxes;

    private Map<String, JPrimitiveType> primitivesByName = new LinkedHashMap<>();
    private Map<JPrimitiveType, JClassNamespace> boxByPrimitive = new LinkedHashMap<>();
    private Map<JClassNamespace, JPrimitiveType> primitiveByBox = new LinkedHashMap<>();

    public JInMemoryTypeConstants initPrimitiveMaps() {
        this.allPrimitives = List.of(booleanType, byteType, shortType, charType, intType, longType, floatType, doubleType);
        List<JClassReference> allBoxes = new ArrayList<>();

        for (JPrimitiveType type : this.allPrimitives) {
            allBoxes.add(type.box());
            boxByPrimitive.put(type, type.boxNamespace());
            primitiveByBox.put(type.boxNamespace(), type);
            primitivesByName.put(type.name(), type);
        }

        this.allBoxes = Collections.unmodifiableList(allBoxes);
        this.boxByPrimitive = Collections.unmodifiableMap(boxByPrimitive);
        this.primitivesByName = Collections.unmodifiableMap(primitivesByName);
        this.primitiveByBox = Collections.unmodifiableMap(primitiveByBox);
        return this;
    }

    public JInMemoryTypeConstants setObject(JClassReference object) {
        this.object = object;
        return this;
    }

    public JInMemoryTypeConstants setCloneable(JClassReference cloneable) {
        this.cloneable = cloneable;
        return this;
    }

    public JInMemoryTypeConstants setSerializable(JClassReference serializable) {
        this.serializable = serializable;
        return this;
    }

    public JInMemoryTypeConstants setRuntimeException(JClassReference runtimeException) {
        this.runtimeException = runtimeException;
        return this;
    }

    public JInMemoryTypeConstants setVoidType(JNoneType voidType) {
        this.voidType = voidType;
        return this;
    }

    public JInMemoryTypeConstants setNullType(JNoneType nullType) {
        this.nullType = nullType;
        return this;
    }

    public JInMemoryTypeConstants setNoneType(JNoneType noneType) {
        this.noneType = noneType;
        return this;
    }

    public JInMemoryTypeConstants setVoidBox(JClassReference voidBox) {
        this.voidBox = voidBox;
        return this;
    }

    public JInMemoryTypeConstants setBooleanType(JPrimitiveType booleanType) {
        this.booleanType = booleanType;
        return this;
    }

    public JInMemoryTypeConstants setByteType(JPrimitiveType byteType) {
        this.byteType = byteType;
        return this;
    }

    public JInMemoryTypeConstants setShortType(JPrimitiveType shortType) {
        this.shortType = shortType;
        return this;
    }

    public JInMemoryTypeConstants setCharType(JPrimitiveType charType) {
        this.charType = charType;
        return this;
    }

    public JInMemoryTypeConstants setIntType(JPrimitiveType intType) {
        this.intType = intType;
        return this;
    }

    public JInMemoryTypeConstants setLongType(JPrimitiveType longType) {
        this.longType = longType;
        return this;
    }

    public JInMemoryTypeConstants setFloatType(JPrimitiveType floatType) {
        this.floatType = floatType;
        return this;
    }

    public JInMemoryTypeConstants setDoubleType(JPrimitiveType doubleType) {
        this.doubleType = doubleType;
        return this;
    }

    @Override
    public List<JPrimitiveType> allPrimitives() {
        return allPrimitives;
    }

    @Override
    public Map<String, JPrimitiveType> primitivesByName() {
        return this.primitivesByName;
    }

    @Override
    public Map<JPrimitiveType, JClassNamespace> boxByPrimitive() {
        return this.boxByPrimitive;
    }

    @Override
    public Map<JClassNamespace, JPrimitiveType> primitiveByBox() {
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
