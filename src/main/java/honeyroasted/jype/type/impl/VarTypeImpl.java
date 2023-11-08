package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class VarTypeImpl extends AbstractPossiblyUnmodifiableType implements VarType {
    private TypeParameterLocation location;
    private Set<Type> upperBounds = new LinkedHashSet<>();

    public VarTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        VarType copy = new VarTypeImpl(this.typeSystem());
        cache.put(this, copy);

        copy.setLocation(this.location);
        copy.setUpperBounds(this.upperBounds.stream().map(t -> (Type) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBounds = WildType.linkedCopyOf(this.upperBounds);
    }

    @Override
    protected void makeModifiable() {
        this.upperBounds = new LinkedHashSet<>(this.upperBounds);
    }

    @Override
    public TypeParameterLocation location() {
        return this.location;
    }

    @Override
    public void setLocation(TypeParameterLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public Set<Type> upperBounds() {
        return this.upperBounds;
    }

    @Override
    public void setUpperBounds(Set<Type> upperBounds) {
        super.checkUnmodifiable();
        this.upperBounds = upperBounds;
    }

    @Override
    public boolean equals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;
        seen = Type.concat(seen, this);

        if (other instanceof VarType vt) {
            return Objects.equals(location, vt.location()) &&
                    Type.equals(upperBounds, vt.upperBounds(), seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.multiHash(Objects.hashCode(location), Type.hashCode(upperBounds, seen));
    }

}
