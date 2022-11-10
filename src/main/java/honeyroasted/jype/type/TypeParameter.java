package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class represents a type parameter. It can be used interchangeably as a formal type parameter in
 * {@link TypeDeclaration} or as a reference to a type parameter. Notably, unlike most other {@link Type}s,
 * equality between references of {@link TypeParameter} is based on instance equality. If a {@link TypeParameter}
 * is being used as a reference to a formal type parameter, it should be <i>the same instance</i> of this class
 * being used in both places.
 */
public class TypeParameter extends AbstractType implements TypeConcrete {
    private String name;
    private TypeConcrete upperBound;
    private TypeConcrete lowerBound;

    private boolean mutable = true;

    /**
     * Creates a new {@link TypeParameter}.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param name   The name of this {@link TypeParameter}
     * @param upperBound  The upper bound of this {@link TypeParameter}
     * @param lowerBound The lower bound of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, String name, TypeConcrete upperBound, TypeConcrete lowerBound) {
        super(system);
        this.name = name;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    /**
     * Creates a new {@link TypeParameter} with no name.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param upperBound  The upper bound of this {@link TypeParameter}
     * @param lowerBound The lower bound of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, TypeConcrete upperBound, TypeConcrete lowerBound) {
        this(system, "<UNKNOWN_NAME>", upperBound, lowerBound);
    }

    /**
     * Creates a new {@link TypeParameter} with no bounds.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param name   The name of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, String name) {
        this(system, name, system.of(Object.class).get(), system.NULL);
    }

    /**
     * @return The name of this {@link TypeParameter}
     */
    public String name() {
        return this.name;
    }

    /**
     * @return The upper bound of this {@link TypeParameter}
     */
    public TypeConcrete upperBound() {
        return this.upperBound;
    }

    /**
     * @return The lower bound of this {@link TypeParameter}
     */
    public TypeConcrete lowerBound() {
        return this.lowerBound;
    }

    /**
     * @return A {@link String} representing this specific {@link TypeParameter} instance, for use in differentiating
     * {@link TypeParameter}s with the same name
     */
    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Sets the upper bound of this {@link TypeParameter} if it is currently mutable
     *
     * @param upperBound The new bound
     * @throws IllegalStateException if this {@link TypeParameter} is no longer mutable
     */
    public void setUpperBound(TypeConcrete upperBound) {
        if (this.mutable) {
            this.upperBound = upperBound;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    /**
     * Sets the lower bound of this {@link TypeParameter} if it is currently mutable
     *
     * @param lowerBound The new bound
     * @throws IllegalStateException if this {@link TypeParameter} is no longer mutable
     */
    public void setLowerBound(TypeConcrete lowerBound) {
        if (this.mutable) {
            this.lowerBound = lowerBound;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    /**
     * Sets the name of this {@link TypeParameter} if it is currently mutable
     *
     * @param name The new name
     * @throws IllegalStateException if this {@link TypeParameter} is no longer mutable
     */
    public void setName(String name) {
        if (this.mutable) {
            this.name = name;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful("T" + this.name + ";", getClass(), TypeString.Target.SIGNATURE);
        } else {
            TypeString bound = this.upperBound.toSignature(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + ":" + bound.value(), getClass(), TypeString.Target.SIGNATURE) : bound;
        }
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeParameter.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.name, getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.upperBound.toSource(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + " extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
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

    /**
     * This class represents a {@link TypeParameter} that isn't defined in a {@link TypeDeclaration}. It can be used
     * as an inference variable. For example, the diamond operator in {@code new ArrayList<>()} would be represented
     * as a {@link TypeParameterized} with one argument, a TypeParameter.Placeholder.
     */
    public static class Fresh extends TypeParameter {

        /**
         * Creates a new {@link Fresh}
         *
         * @param system The {@link TypeSystem} this {@link Fresh} is a member of
         * @param upperBound  The upper bound of this {@link Fresh}
         * @param lowerBound The lower bound of this {@link Fresh}
         */
        public Fresh(TypeSystem system, TypeConcrete upperBound, TypeConcrete lowerBound) {
            super(system, "#typevar", upperBound, lowerBound);
        }

        /**
         * Creates a new {@link Fresh} with no bound
         *
         * @param system The {@link TypeSystem} this {@link Fresh} is a member of
         */
        public Fresh(TypeSystem system) {
            this(system, system.OBJECT, system.NULL);
        }

        @Override
        public TypeString toSignature(TypeString.Context context) {
            return TypeString.failure(Fresh.class, TypeString.Target.SIGNATURE);
        }

        @Override
        public TypeString toDescriptor(TypeString.Context context) {
            return TypeString.failure(Fresh.class, TypeString.Target.DESCRIPTOR);
        }

        @Override
        public TypeString toSource(TypeString.Context context) {
            return TypeString.failure(Fresh.class, TypeString.Target.SOURCE);
        }

    }

}
