package honeyroasted.jype.system;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.List;

public record TypeConstants(ClassReference object,
                            NoneType voidType, NoneType nullType, NoneType noneType,
                            PrimitiveType booleanType, PrimitiveType byteType, PrimitiveType shortType,
                            PrimitiveType charType, PrimitiveType intType, PrimitiveType longType,
                            PrimitiveType floatType, PrimitiveType doubleType) {

    public List<PrimitiveType> allPrimitives() {
        return List.of(booleanType, byteType, shortType, charType, intType, longType, floatType, doubleType);
    }

}
