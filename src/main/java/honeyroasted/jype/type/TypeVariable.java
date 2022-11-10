package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Set;
import java.util.function.Consumer;

public class TypeVariable extends AbstractType implements TypeConcrete {
    private String name;
    private TypeConcrete upperBound;
    private TypeConcrete lowerBound;

    private boolean mutable = true;

    /**
     * Creates a new {@link TypeVariable}.
     *
     * @param system The {@link TypeSystem} this {@link TypeVariable} is a member of
     * @param name   The name of this {@link TypeVariable}
     * @param upperBound  The upper bound of this {@link TypeVariable}
     * @param lowerBound The lower bound of this {@link TypeVariable}
     */
    public TypeVariable(TypeSystem system, String name, TypeConcrete upperBound, TypeConcrete lowerBound) {
        super(system);
        this.name = name;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    /**
     * Creates a new {@link TypeVariable} with no name.
     *
     * @param system The {@link TypeSystem} this {@link TypeVariable} is a member of
     * @param upperBound  The upper bound of this {@link TypeVariable}
     * @param lowerBound The lower bound of this {@link TypeVariable}
     */
    public TypeVariable(TypeSystem system, TypeConcrete upperBound, TypeConcrete lowerBound) {
        this(system, "#typevar", upperBound, lowerBound);
    }

    /**
     * Creates a new {@link TypeVariable} with no bounds.
     *
     * @param system The {@link TypeSystem} this {@link TypeVariable} is a member of
     * @param name   The name of this {@link TypeVariable}
     */
    public TypeVariable(TypeSystem system, String name) {
        this(system, name, system.of(Object.class).get(), system.NULL);
    }

    public TypeVariable(TypeSystem system) {
        this(system, "#typevar");
    }

    /**
     * @return The name of this {@link TypeVariable}
     */
    public String name() {
        return this.name;
    }

    /**
     * @return The upper bound of this {@link TypeVariable}
     */
    public TypeConcrete upperBound() {
        return this.upperBound;
    }

    /**
     * @return The lower bound of this {@link TypeVariable}
     */
    public TypeConcrete lowerBound() {
        return this.lowerBound;
    }

    /**
     * @return A {@link String} representing this specific {@link TypeVariable} instance, for use in differentiating
     * {@link TypeVariable}s with the same name
     */
    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Sets the upper bound of this {@link TypeVariable} if it is currently mutable
     *
     * @param upperBound The new bound
     * @throws IllegalStateException if this {@link TypeVariable} is no longer mutable
     */
    public void setUpperBound(TypeConcrete upperBound) {
        if (this.mutable) {
            this.upperBound = upperBound;
        } else {
            throw new IllegalStateException("TypeVariable is no longer mutable");
        }
    }

    /**
     * Sets the lower bound of this {@link TypeVariable} if it is currently mutable
     *
     * @param lowerBound The new bound
     * @throws IllegalStateException if this {@link TypeVariable} is no longer mutable
     */
    public void setLowerBound(TypeConcrete lowerBound) {
        if (this.mutable) {
            this.lowerBound = lowerBound;
        } else {
            throw new IllegalStateException("TypeVariable is no longer mutable");
        }
    }

    /**
     * Sets the name of this {@link TypeVariable} if it is currently mutable
     *
     * @param name The new name
     * @throws IllegalStateException if this {@link TypeVariable} is no longer mutable
     */
    public void setName(String name) {
        if (this.mutable) {
            this.name = name;
        } else {
            throw new IllegalStateException("TypeVariable is no longer mutable");
        }
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.failure(getClass(), TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(getClass(), TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure(getClass(), TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.name + "-" + this.identity(), getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.upperBound.toReadable(TypeString.Context.CONCRETE);
            TypeString lower = this.lowerBound.toReadable(TypeString.Context.CONCRETE);

            if (bound.successful() && lower.successful()) {
                return TypeString.successful(this.name + "-" + this.identity() + " extends " + bound.value() + ", super " + lower.value(), getClass(), TypeString.Target.SOURCE);
            } else {
                return bound.successful() ? lower : bound;
            }
        }
    }

    @Override
    public void lock() {
        this.mutable = false;
    }

    @Override
    public void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
            this.upperBound.forEach(consumer, seen);
        }
    }

    @Override
    public boolean isProperType() {
        return false;
    }

    @Override
    public Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen) {
        return seen.contains(this) ? Set.of(this) : this.upperBound.circularChildren(seen, this);
    }

    @Override
    public boolean equalsExactly(TypeConcrete other) {
        return this == other;
    }

    @Override
    public int hashCodeExactly() {
        return System.identityHashCode(this);
    }
    
}
