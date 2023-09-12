package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

public sealed abstract class AbstractType implements Type permits honeyroasted.jype.type.ArrayType, honeyroasted.jype.type.NoneType, honeyroasted.jype.type.PrimitiveType, honeyroasted.jype.type.WildType {
    private TypeSystem typeSystem;

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
