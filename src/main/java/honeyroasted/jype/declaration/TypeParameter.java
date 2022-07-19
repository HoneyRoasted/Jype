package honeyroasted.jype.declaration;

import honeyroasted.jype.Type;
import honeyroasted.jype.concrete.TypeConcrete;

public class TypeParameter implements Type {
    private String name;
    private TypeConcrete bound;

    public TypeParameter(String name, TypeConcrete bound) {
        this.name = name;
        this.bound = bound;
    }

    @Override
    public String toString() {
        return this.name + " extends " + this.bound.toString();
    }

}
