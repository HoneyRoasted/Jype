package honeyroasted.jype.declaration;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;

import java.util.function.Function;

public class TypeParameter implements Type {
    private String name;
    private TypeConcrete bound;

    private boolean mutable = true;

    public TypeParameter(String name, TypeConcrete bound) {
        this.name = name;
        this.bound = bound;
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

    public void setBound(TypeConcrete bound) {
        if (this.mutable) {
            this.bound = bound;
        } else {
            throw new IllegalStateException("TypeParameter is no longer mutable");
        }
    }

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeParameter(this.name, this.bound.map(mapper)));
    }

    @Override
    public void lock() {
        this.mutable = mutable;
        this.bound.lock();
    }

    @Override
    public String toString() {
        return this.name + " extends " + this.bound.toString();
    }

}
