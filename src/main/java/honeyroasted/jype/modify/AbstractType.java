package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.*;

public sealed abstract class AbstractType implements Type permits ArrayType, NoneType, PrimitiveType, WildType {
    private TypeSystem typeSystem;

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }
}
