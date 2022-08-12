package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

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
    private TypeConcrete bound;

    private boolean mutable = true;

    /**
     * Creates a new {@link TypeParameter}.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param name   The name of this {@link TypeParameter}
     * @param bound  The bound of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, String name, TypeConcrete bound) {
        super(system);
        this.name = name;
        this.bound = bound.flatten();
    }

    /**
     * Creates a new {@link TypeParameter} with no name.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param bound  The bound of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, TypeConcrete bound) {
        this(system, "<UNKNOWN_NAME>", bound);
    }

    /**
     * Creates a new {@link TypeParameter} with no bounds.
     *
     * @param system The {@link TypeSystem} this {@link TypeParameter} is a member of
     * @param name   The name of this {@link TypeParameter}
     */
    public TypeParameter(TypeSystem system, String name) {
        this(system, name, system.OBJECT);
    }

    /**
     * @return The name of this {@link TypeParameter}
     */
    public String name() {
        return this.name;
    }

    /**
     * @return The bound of this {@link TypeParameter}
     */
    public TypeConcrete bound() {
        return this.bound;
    }

    /**
     * @return A {@link String} representing this specific {@link TypeParameter} instance, for use in differentiating
     * {@link TypeParameter}s with the same name
     */
    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Sets the bound of this {@link TypeParameter} if it is currently mutable
     *
     * @param bound The new bound
     * @throws IllegalStateException if this {@link TypeParameter} is no longer mutable
     */
    public void setBound(TypeConcrete bound) {
        if (this.mutable) {
            this.bound = bound;
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
            TypeString bound = this.bound.toSignature(TypeString.Context.CONCRETE);
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
            TypeString bound = this.bound.toSource(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + " extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.name, getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.bound.toReadable(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + " extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
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
            this.bound.forEach(consumer, seen);
        }
    }

    @Override
    public boolean isProperType() {
        return false;
    }

    @Override
    public boolean isCircular(Set<TypeConcrete> seen) {
        return seen.contains(this) || this.bound.isCircular(seen, this);
    }

    @Override
    public boolean equalsExactly(TypeConcrete other) {
        return this == other;
    }

    @Override
    public int hashCodeExactly() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return this.name + "-" + identity();
    }

    /**
     * This class represents a {@link TypeParameter} that isn't defined in a {@link TypeDeclaration}. It can be used
     * as an inference variable. For example, the diamond operator in {@code new ArrayList<>()} would be represented
     * as a {@link TypeClass} with one argument, a TypeParameter.Placeholder.
     */
    public static class Placeholder extends TypeParameter {

        /**
         * Creates a new {@link Placeholder}
         *
         * @param system The {@link TypeSystem} this {@link Placeholder} is a member of
         * @param bound  The bound of this {@link Placeholder}
         */
        public Placeholder(TypeSystem system, TypeConcrete bound) {
            super(system, "#typevar", bound);
        }

        /**
         * Creates a new {@link Placeholder} with no bound
         *
         * @param system The {@link TypeSystem} this {@link Placeholder} is a member of
         */
        public Placeholder(TypeSystem system) {
            this(system, system.OBJECT);
        }

        @Override
        public TypeString toSignature(TypeString.Context context) {
            return TypeString.failure(Placeholder.class, TypeString.Target.SIGNATURE);
        }

        @Override
        public TypeString toDescriptor(TypeString.Context context) {
            return TypeString.failure(Placeholder.class, TypeString.Target.DESCRIPTOR);
        }

        @Override
        public TypeString toSource(TypeString.Context context) {
            return TypeString.failure(Placeholder.class, TypeString.Target.SOURCE);
        }

        @Override
        public TypeString toReadable(TypeString.Context context) {
            if (context == TypeString.Context.CONCRETE) {
                return TypeString.successful(name() + "-" + identity(), getClass(), TypeString.Target.SOURCE);
            } else {
                TypeString bound = this.bound().toSource(TypeString.Context.CONCRETE);
                return bound.successful() ? TypeString.successful(this.name() + "-" + identity() + " extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
            }
        }
    }

}
