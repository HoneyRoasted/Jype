package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.marker.TypeComposition;
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
 * This class represents an intersection type. That is, a type which is every one of its child types. Given an intersection
 * type A, and some other type T, the following generally holds true:
 * <ul>
 * <li>T is assignable to A if, for <i>each</i> type A<sub>i</sub> in A, T is assignable to A<sub>i</sub> </li>
 * <li>A is assignable to T if, for <i>any</i> type A<sub>i</sub> in A, A<sub>i</sub> is assignable to T</li>
 * </ul>
 */
public class TypeAnd extends AbstractType implements TypeComposition {
    private Set<TypeConcrete> types;

    /**
     * Creates a new {@link TypeAnd}.
     *
     * @param system The {@link TypeSystem} this {@link TypeAnd} is a member of
     * @param types  The set of {@link TypeConcrete}s that this represents the intersection of
     */
    public TypeAnd(TypeSystem system, Set<TypeConcrete> types) {
        super(system);
        this.types = types;
    }

    /**
     * @return The set of {@link TypeConcrete}s that this {@link TypeAnd} is an intersection of
     */
    public Set<TypeConcrete> types() {
        return types;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSignature(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(":")), getClass(), TypeString.Target.SIGNATURE));
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeAnd.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toSource(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(" & ")), getClass(), TypeString.Target.SOURCE));
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        List<TypeString> args = this.types.stream().map(t -> t.toReadable(context)).toList();
        return args.stream().filter(t -> !t.successful()).findFirst().orElseGet(() ->
                TypeString.successful(args.stream().map(TypeString::value).collect(Collectors.joining(" & ")), getClass(), TypeString.Target.READABLE));
    }

    @Override
    public void lock() {
        this.types = Collections.unmodifiableSet(new LinkedHashSet<>(this.types));
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeAnd(this.typeSystem(), this.types.stream().map(t -> (TypeConcrete) t.map(mapper)).collect(Collectors.toCollection(LinkedHashSet::new))));
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
        Set<TypeConcrete> flattened = new LinkedHashSet<>();
        this.types.stream().map(TypeConcrete::flatten).forEach(t -> {
            if (t instanceof TypeAnd and) {
                flattened.addAll(and.types());
            } else {
                flattened.add(t);
            }
        });

        if (flattened.size() == 0) {
            return this.typeSystem().VOID;
        } else if (flattened.size() == 1) {
            return flattened.iterator().next();
        } else {
            //Object is never useful in intersection type
            //under the Java type system
            flattened.remove(this.typeSystem().OBJECT);
            return new TypeAnd(this.typeSystem(), flattened);
        }
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
