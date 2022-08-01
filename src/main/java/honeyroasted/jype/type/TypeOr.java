package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeOr implements TypeConcrete {
    private Set<TypeConcrete> types;

    public TypeOr(Set<TypeConcrete> types) {
        this.types = types;
    }

    public TypeOr() {
        this(new LinkedHashSet<>());
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
        this.types = Set.copyOf(this.types);
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeOr(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).collect(Collectors.toSet())));
    }

    public Set<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeConcrete flatten() {
        Set<TypeConcrete> flattened = new HashSet<>();
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
            return flattened.iterator().next();
        } else {
            return new TypeOr(flattened);
        }
    }


    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system) {
        return new TypeConstraint.And(this.types.stream().map(t -> t.assignabilityTo(other, system)).toList());
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