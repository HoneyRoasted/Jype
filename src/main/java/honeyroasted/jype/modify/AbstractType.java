package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.Type;

public abstract class AbstractType implements Type {
    private TypeSystem typeSystem;

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public String toString() {
        return TypeVisitors.TO_STRING_DETAIL.visit(this);
    }

    @Override
    public String simpleName() {
        return TypeVisitors.TO_STRING_SIMPLE.visit(this);
    }
}
