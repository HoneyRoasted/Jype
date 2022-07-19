package honeyroasted.jype.concrete;

import java.util.ArrayList;
import java.util.List;

public class TypeAnd implements TypeConcrete {
    private List<TypeConcrete> types;

    public TypeAnd(List<TypeConcrete> types) {
        this.types = List.copyOf(types);
    }

    public List<TypeConcrete> types() {
        return types;
    }

    public TypeConcrete flatten() {
        if (this.types.isEmpty()) {
            return TypeNone.VOID;
        } else if (this.types.size() == 1) {
            TypeConcrete type = this.types.get(0);
            return type instanceof TypeAnd intersection ? intersection.flatten() :
                    type instanceof TypeOr union ? union.flatten() :
                            type;
        } else {
            List<TypeConcrete> types = new ArrayList<>();
            this.types.forEach(t -> flatten(t, types));
            return new TypeAnd(types);
        }
    }

    private static void flatten(TypeConcrete type, List<TypeConcrete> types) {
        if (type instanceof TypeAnd intersection) {
            intersection.types().forEach(t -> flatten(t, types));
        } else {
            types.add(type);
        }
    }

}
