package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.metadata.location.JFieldLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JType;

import java.util.Optional;
import java.util.Set;

public class JFieldReferenceImpl extends JAbstractPossiblyUnmodifiableType implements JFieldReference {
    private JFieldLocation location;
    private JClassReference outerClass;
    private JType type;
    private int modifiers;

    public JFieldReferenceImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JFieldReference copy = this.typeSystem().typeFactory().newFieldReference();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setLocation(this.location);
        copy.setOuterClass(this.outerClass.copy(cache));
        copy.setType(this.type.copy(cache));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    public JFieldLocation location() {
        return this.location;
    }

    @Override
    public void setLocation(JFieldLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public JClassReference outerClass() {
        return this.outerClass;
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        this.checkUnmodifiable();
        this.outerClass = outerClass;
    }

    @Override
    public JType type() {
        return this.type;
    }

    @Override
    public void setType(JType type) {
        super.checkUnmodifiable();
        this.type = type;
    }

    @Override
    public int modifiers() {
        return this.modifiers;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.checkUnmodifiable();
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof JFieldReference ref) {
            return ref.location().equals(this.location);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;

        return this.location.hashCode();
    }

}
