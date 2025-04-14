package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class JVarTypeImpl extends JAbstractPossiblyUnmodifiableType implements JVarType {
    private JTypeParameterLocation location;
    private Set<JType> upperBounds = new LinkedHashSet<>();

    public JVarTypeImpl(JTypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JVarType copy = this.typeSystem().typeFactory().newVarType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setLocation(this.location);
        copy.setUpperBounds(this.upperBounds.stream().map(t -> (JType) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBounds = JWildType.linkedCopyOf(this.upperBounds);
    }

    @Override
    protected void makeModifiable() {
        this.upperBounds = new LinkedHashSet<>(this.upperBounds);
    }

    @Override
    public JTypeParameterLocation location() {
        return this.location;
    }

    @Override
    public void setLocation(JTypeParameterLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public Set<JType> upperBounds() {
        return this.upperBounds;
    }

    @Override
    public void setUpperBounds(Set<JType> upperBounds) {
        super.checkUnmodifiable();
        this.upperBounds = upperBounds;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JVarType vt) {
            return Objects.equals(location, vt.location()) &&
                    JType.equals(upperBounds, vt.upperBounds(), kind, seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        seen = JType.concat(seen, this);

        return JType.multiHash(Objects.hashCode(location), JType.hashCode(upperBounds, seen));
    }

}
