package honeyroasted.jype.concrete;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TypeAnd implements TypeConcrete {
    private List<TypeConcrete> types;

    public TypeAnd(List<TypeConcrete> types) {
        this.types = types;
    }

    public TypeAnd() {
        this(new ArrayList<>());
    }

    @Override
    public void lock() {
        this.types = List.copyOf(this.types);
        this.types.forEach(Type::lock);
    }

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeAnd(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
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

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return new Constraint.Or(this.types.stream().map(t -> t.assignabilityTo(other)).toList());
    }
}
