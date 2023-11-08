package honeyroasted.jype.modify;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.IdentityHashMap;

public abstract class AbstractPossiblyUnmodifiableType extends AbstractPossiblyUnmodifiable implements Type {
    private TypeSystem typeSystem;

    public AbstractPossiblyUnmodifiableType(TypeSystem typeSystem) {
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

    @Override
    public int hashCode() {
        return this.hashCode(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Type t) return this.equals(t, Collections.newSetFromMap(new IdentityHashMap<>()));
        return false;
    }
}
