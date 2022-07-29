package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;

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
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.failure("TypeOr", "signature");
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure("TypeOr", "descriptor");
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure("TypeOr", "source");
    }

    @Override
    public void lock() {
        this.types = List.copyOf(this.types);
        this.types.forEach(Type::lock);
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeOr(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).collect(Collectors.toList())));
    }

    public List<TypeConcrete> types() {
        return types;
    }

    public TypeConcrete flatten() {
        if (this.types.isEmpty()) {
            return TypeNone.VOID;
        } else if (this.types.size() == 1) {
            TypeConcrete type = this.types.get(0);
            return type instanceof TypeAnd ? ((TypeAnd) type).flatten() :
                    type instanceof TypeOr ? ((TypeOr) type).flatten() :
                            type;
        } else {
            List<TypeConcrete> types = new ArrayList<>();
            this.types.forEach(t -> flatten(t, types));
            return new TypeOr(types);
        }
    }

    private static void flatten(TypeConcrete type, List<TypeConcrete> types) {
        if (type instanceof TypeOr) {
            ((TypeOr) type).types().forEach(t -> flatten(t, types));
        } else {
            types.add(type);
        }
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return new TypeConstraint.And(this.types.stream().map(t -> t.assignabilityTo(other)).collect(Collectors.toList()));
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