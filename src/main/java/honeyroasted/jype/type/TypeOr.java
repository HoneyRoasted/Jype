package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class represents a union type. That is, a type which is at least one of its children types. Given a union type
 * O, and some other type T, the following generally holds true:
 * <ul>
 * <li>T is assignable to O if, for <i>any</i> type O<sub>i</sub> in O, T is assignable to O<sub>i</sub></li>
 * <li>O is assignable to T if, for <i>each</i> type O<sub>i</sub> in O, O<sub>i</sub> is assignable to T</li>
 * </ul>
 */
public class TypeOr extends AbstractType implements TypeConcrete {
    private Set<TypeConcrete> types;

    /**
     * Creates a new {@link TypeOr}
     *
     * @param system The {@link TypeSystem} this {@link TypeOr} is a member of
     * @param types  The set of {@link TypeConcrete}s that this represents the union of
     */
    public TypeOr(TypeSystem system, Set<TypeConcrete> types) {
        super(system);
        this.types = types;
    }

    /**
     * @return The set of {@link TypeConcrete}s that this {@link TypeOr} is an union of
     */
    public Set<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.failure(TypeOr.class, TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeOr.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure(TypeOr.class, TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toReadable(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(" | ")), getClass(), TypeString.Target.READABLE));
    }

    @Override
    public void lock() {
        this.types = Collections.unmodifiableSet(new LinkedHashSet<>(this.types));
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeOr(this.typeSystem(), this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).collect(Collectors.toSet())));
    }

    @Override
    public void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
            this.types().forEach(t -> t.forEach(consumer, seen));
        }
    }

    @Override
    public boolean isProperType() {
        return this.types.stream().allMatch(TypeConcrete::isProperType);
    }

    @Override
    public Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen) {
        Set<TypeConcrete> res = new HashSet<>();
        this.types.stream().map(t -> t.circularChildren(seen, this)).forEach(res::addAll);
        return res;
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
            return this.typeSystem().VOID;
        } else if (flattened.size() == 1) {
            return flattened.iterator().next();
        } else {
            return new TypeOr(this.typeSystem(), flattened);
        }
    }

    @Override
    public String toString() {
        return this.types.stream().map(TypeConcrete::toString).collect(Collectors.joining(" | "));
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeOr typeOr = (TypeOr) o;

        return Objects.equals(types, typeOr.types);
    }

    @Override
    public int hashCodeExactly() {
        return types != null ? types.hashCode() : 0;
    }
}