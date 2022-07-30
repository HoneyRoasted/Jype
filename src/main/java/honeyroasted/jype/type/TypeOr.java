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
        return (T) mapper.apply(new TypeOr(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
    }

    public List<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeConcrete flatten() {
        List<TypeConcrete> flattened = new ArrayList<>();
        this.types.stream().map(TypeConcrete::flatten).forEach(t -> {
            if (t instanceof TypeOr or) {
                flattened.addAll(or.types());
            } else {
                flattened.add(t);
            }
        });

        if (flattened.size() == 0) {
            return TypeNone.VOID;
        } else if (flattened.size() == 1) {
            return flattened.get(0);
        } else {
            return new TypeOr(flattened);
        }
    }


    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return new TypeConstraint.And(this.types.stream().map(t -> t.assignabilityTo(other)).toList());
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