package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeAnd implements TypeConcrete {
    private List<TypeConcrete> types;

    public TypeAnd(List<TypeConcrete> types) {
        this.types = types;
    }

    public TypeAnd() {
        this(new ArrayList<>());
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSignature(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(":"))));
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure("TypeAnd", "descriptor");
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSource(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(" & "))));
    }

    @Override
    public void lock() {
        this.types = List.copyOf(this.types);
        this.types.forEach(Type::lock);
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeAnd(this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
    }

    public List<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeConcrete flatten() {
        List<TypeConcrete> flattened = new ArrayList<>();
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
            return flattened.get(0);
        } else {
            return new TypeOr(flattened);
        }
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return new TypeConstraint.Or(this.types.stream().map(t -> t.assignabilityTo(other)).toList());
    }

    @Override
    public String toString() {
        return this.types.stream().map(TypeConcrete::toString).collect(Collectors.joining(" & "));
    }
}
