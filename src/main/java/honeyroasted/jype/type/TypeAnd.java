package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeAnd extends AbstractType implements TypeConcrete {
    private Set<TypeConcrete> types;

    public TypeAnd(Set<TypeConcrete> types) {
        this.types = types;
    }

    public TypeAnd() {
        this(new LinkedHashSet<>());
    }

    public Set<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSignature(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(":"))));
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeAnd.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSource(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(" & "))));
    }

    @Override
    public void lock() {
        this.types = Collections.unmodifiableSet(new LinkedHashSet<>(this.types));
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeAnd(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).collect(Collectors.toSet())));
    }

    @Override
    public TypeConcrete flatten() {
        Set<TypeConcrete> flattened = new LinkedHashSet<>();
        this.types.stream().map(TypeConcrete::flatten).forEach(t -> {
            if (t instanceof TypeAnd and) {
                flattened.addAll(and.types());
            } else {
                flattened.add(t);
            }
        });

        if (flattened.size() == 0) {
            return TypeNone.VOID;
        } else if (flattened.size() == 1) {
            return flattened.iterator().next();
        } else {
            return new TypeAnd(flattened);
        }
    }

    @Override
    public String toString() {
        return this.types.stream().map(TypeConcrete::toString).collect(Collectors.joining(" & "));
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeAnd typeAnd = (TypeAnd) o;

        return Objects.equals(types, typeAnd.types);
    }

    @Override
    public int hashCodeExactly() {
        return types != null ? types.hashCode() : 0;
    }
}
