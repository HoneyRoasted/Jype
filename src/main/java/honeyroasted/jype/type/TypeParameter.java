package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;

import java.util.function.Function;

public class TypeParameter extends AbstractType implements TypeConcrete {
    private String name;
    private TypeConcrete bound;

    private boolean mutable = true;

    public TypeParameter(TypeConcrete bound) {
        this.bound = bound;
    }

    public TypeParameter(String name, TypeConcrete bound) {
        this.name = name;
        this.bound = bound.flatten();
    }

    public TypeParameter(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public TypeConcrete bound() {
        return this.bound;
    }

    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    public void setBound(TypeConcrete bound) {
        if (this.mutable) {
            this.bound = bound;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    public void setName(String name) {
        if (this.mutable) {
            this.name = name;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeParameter(this.name, this.bound.map(mapper)));
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful("T" + this.name + ";");
        } else {
            TypeString bound = this.bound.toSignature(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + ":" + bound.value()) : bound;
        }
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeParameter.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.name);
        } else {
            TypeString bound = this.bound.toSource(TypeString.Context.CONCRETE);
            return bound.successful() ? TypeString.successful(this.name + " extends " + bound.value()) : bound;
        }
    }

    @Override
    public void lock() {
        this.mutable = false;
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

    public static class Placeholder extends TypeParameter {

        public Placeholder(TypeConcrete bound) {
            super("#typevar", bound);
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
    }

}
