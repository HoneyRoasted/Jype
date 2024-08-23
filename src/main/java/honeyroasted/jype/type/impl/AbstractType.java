package honeyroasted.jype.type.impl;

import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;

public abstract class AbstractType implements Type {
    private TypeSystem typeSystem;
    private PropertySet metadata = new PropertySet();

    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public PropertySet metadata() {
        return this.metadata;
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
        if (o instanceof Type t) return this.equals(t, Equality.STRUCTURAL, new HashSet<>());
        return false;
    }
}
