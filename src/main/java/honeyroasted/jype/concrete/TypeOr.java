package honeyroasted.jype.concrete;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeOr implements TypeConcrete {
    private List<TypeConcrete> types;

    public TypeOr(List<TypeConcrete> types) {
        this.types = types;
    }

    public TypeOr() {
        this(new ArrayList<>());
    }

    @Override
    public void lock() {
        this.types = List.copyOf(this.types);
        this.types.forEach(Type::lock);
    }

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeOr(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
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

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return new Constraint.And(this.types.stream().map(t -> t.assignabilityTo(other)).toList());
    }

    @Override
    public String toString() {
        return this.types.stream().map(TypeConcrete::toString).collect(Collectors.joining(" | "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeOr typeOr = (TypeOr) o;

        return Objects.equals(types, typeOr.types);
    }

    @Override
    public int hashCode() {
        return types != null ? types.hashCode() : 0;
    }
}