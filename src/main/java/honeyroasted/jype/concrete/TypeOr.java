package honeyroasted.jype.concrete;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TypeOr implements TypeConcrete {
    private List<TypeConcrete> types;

    public TypeOr(List<TypeConcrete> types) {
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
            return new TypeOr(types);
        }
    }

    private static void flatten(TypeConcrete type, List<TypeConcrete> types) {
        if (type instanceof TypeOr union) {
            union.types().forEach(t -> flatten(t, types));
        } else {
            types.add(type);
        }
    }

}