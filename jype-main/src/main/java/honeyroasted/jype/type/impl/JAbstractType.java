package honeyroasted.jype.type.impl;

import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JType;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;

public abstract class JAbstractType implements JType {
    private JTypeSystem typeSystem;
    private PropertySet metadata = new PropertySet();

    public JAbstractType(JTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public JTypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public PropertySet metadata() {
        return this.metadata;
    }

    @Override
    public String toString() {
        return JTypeVisitors.TO_STRING_DETAIL.visit(this);
    }

    @Override
    public String simpleName() {
        return JTypeVisitors.TO_STRING_SIMPLE.visit(this);
    }

    @Override
    public int hashCode() {
        return this.hashCode(Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof JType t) return this.equals(t, Equality.STRUCTURAL, new HashSet<>());
        return false;
    }
}
