package honeyroasted.jype.concrete;

public class TypeArray implements TypeConcrete {
    private TypeConcrete element;

    public TypeArray(TypeConcrete element) {
        this.element = element;
    }

    public TypeArray(TypeConcrete element, int depth) {
        for (int i = 0; i < depth - 1; i++) {
            element = new TypeArray(element);
        }
        this.element = element;
    }

    public TypeConcrete element() {
        return this.element;
    }

    public TypeConcrete deepElement() {
        return this.element instanceof TypeArray arr ? arr.deepElement() : this.element;
    }

    public int depth() {
        return this.element instanceof TypeArray arr ? arr.depth() + 1 : 1;
    }

    @Override
    public boolean isArray() {
        return true;
    }

}
