package honeyroasted.jype.declaration;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;

import java.util.function.Function;

public class TypeParameter implements Type {
    private String name;
    private TypeConcrete bound;

    public TypeParameter(String name, TypeConcrete bound) {
        this.name = name;
        this.bound = bound;
    }

    public String name() {
        return this.name;
    }

    public TypeConcrete bound() {
        return this.bound;
    }

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeParameter(this.name, this.bound.map(mapper)));
    }

    @Override
    public void lock() {
        this.bound.lock();
    }

    @Override
    public String toString() {
        return this.name + " extends " + this.bound.toString();
    }

}
