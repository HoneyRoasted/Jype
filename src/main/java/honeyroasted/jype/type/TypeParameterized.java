package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class represents a parameterized, concrete type of some object, such as Integer, List&lt;String&gt;, etc.
 * Given some two parameterized types, C and T, the following generally holds true:
 * <ul>
 * <li>C is assignable to T if T is a super class/interface of C and C and T have compatible type arguments</li>
 * <li>Each argument C<sub>i</sub> is compatible with an argument T<sub>i</sub> if they are equal, or if capture conversion succeeds</li>
 * </ul>
 */
public class TypeParameterized extends AbstractType implements TypeConcrete {
    private TypeDeclaration declaration;
    private List<TypeConcrete> arguments;

    private Map<TypeParameter, TypeConcrete> argMap = new LinkedHashMap<>();
    private Map<TypeDeclaration, TypeParameterized> parentMap = new LinkedHashMap<>();

    /**
     * Creates a new {@link TypeParameterized}.
     *
     * @param system      The {@link TypeSystem} this {@link TypeParameterized} is a member of
     * @param declaration The {@link TypeDeclaration} corresponding to this {@link TypeParameterized}
     * @param arguments   The {@link TypeConcrete} arguments for this {@link TypeParameterized}
     */
    public TypeParameterized(TypeSystem system, TypeDeclaration declaration, List<TypeConcrete> arguments) {
        super(system);
        this.declaration = declaration;
        this.arguments = arguments;
    }

    /**
     * Creates a new {@link TypeParameterized} with no {@link TypeConcrete} arguments.
     *
     * @param system      The {@link TypeSystem} this {@link TypeParameterized} is a member of
     * @param declaration The {@link TypeDeclaration} corresponding to this {@link TypeParameterized}
     */
    public TypeParameterized(TypeSystem system, TypeDeclaration declaration) {
        this(system, declaration, new ArrayList<>());
    }

    /**
     * @return The {@link TypeDeclaration} corresponding to this {@link TypeParameterized}
     */
    public TypeDeclaration declaration() {
        return this.declaration;
    }

    /**
     * @return The {@link TypeConcrete} arguments of this {@link TypeParameterized}
     */
    public List<TypeConcrete> arguments() {
        return this.arguments;
    }

    /**
     * Attempts to fetch a {@link TypeConcrete} argument from this {@link TypeParameterized} by its corresponding
     * {@link TypeParameter} instance.
     *
     * @param parameter The {@link TypeParameter} to get the corresponding argument for
     * @return An optional containing the corresponding argument, or an empty optional if no
     * corresponding argument was found
     */
    public Optional<TypeConcrete> argument(TypeParameter parameter) {
        return Optional.ofNullable(this.argMap.computeIfAbsent(parameter, key ->
                this.declaration.parameters().stream().filter(t -> t.equals(key)).findFirst().map(tp -> {
                    int index = this.declaration.parameters().indexOf(tp);
                    return index >= 0 && index < this.arguments.size() ? this.arguments.get(index) :
                            null;
                }).orElse(null)));
    }

    /**
     * Returns a new {@link TypeParameterized}, created from this, relative to the given {@link TypeDeclaration}. For example,
     * if this {@link TypeParameterized} represents the type {@code ArrayList<Integer>}, calling this method with the
     * {@link TypeDeclaration} for {@code List} will produce a {@link TypeParameterized} representing
     * {@code List<Integer>}. This example is a trivial case where the type parameters of {@link ArrayList}
     * are the same, and in the same order, as its parent {@link List}, but in cases where the type
     * parameters don't map directly, this method is useful (and necessary for assignability checking).
     *
     * @param parent The {@link TypeDeclaration} for the resulting {@link TypeParameterized}
     * @return A relative {@link TypeParameterized}, or an empty {@link Optional} if no valid mapping could be found
     */
    public Optional<TypeParameterized> parent(TypeDeclaration parent) {
        return Optional.ofNullable(this.parentMap.computeIfAbsent(parent, key -> this.declaration.pathTo(parent).map(path -> {
            TypeParameterized current = this;
            for (int i = 1; i < path.size(); i++) {
                Optional<TypeParameterized> next = current.directParent(path.get(i));
                if (next.isPresent()) {
                    current = next.get();
                } else {
                    return null;
                }
            }

            return current;
        }).orElse(null)));
    }

    private Optional<TypeParameterized> directParent(TypeDeclaration parent) {
        return this.declaration.parents().stream().filter(t -> t.declaration().equals(parent))
                .findFirst().map(typeParent -> {
                    TypeParameterized result = new TypeParameterized(this.typeSystem(), parent);
                    for (TypeConcrete arg : typeParent.arguments()) {
                        result.arguments().add(arg.map(t -> {
                            if (t instanceof TypeParameter ref && this.argument(ref).isPresent()) {
                                return this.argument(ref).get();
                            }
                            return t;
                        }));
                    }

                    result.lock();
                    return result;
                });
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        StringBuilder sb = new StringBuilder().append("L").append(this.declaration.internalName());
        if (!this.arguments.isEmpty()) {
            List<TypeString> args = this.arguments.stream().map(t -> t.toSignature(TypeString.Context.CONCRETE)).toList();
            Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
            if (failure.isPresent()) {
                return failure.get();
            }

            sb.append("<").append(args.stream().map(TypeString::value).collect(Collectors.joining())).append(">");
        }

        return TypeString.successful(sb.append(";").toString(), getClass(), TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful("L" + this.declaration.namespace().internalName() + ";", getClass(), TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        StringBuilder sb = new StringBuilder().append(this.declaration.name());
        if (!this.arguments.isEmpty()) {
            List<TypeString> args = this.arguments.stream().map(t -> t.toSource(TypeString.Context.CONCRETE)).toList();
            Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
            if (failure.isPresent()) {
                return failure.get();
            }

            sb.append("<").append(args.stream().map(TypeString::value).collect(Collectors.joining(", "))).append(">");
        }

        return TypeString.successful(sb.toString(), getClass(), TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        StringBuilder sb = new StringBuilder().append(this.declaration.namespace().simpleName());
        if (!this.arguments.isEmpty()) {
            List<TypeString> args = this.arguments.stream().map(t -> t.toReadable(TypeString.Context.CONCRETE)).toList();
            Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
            if (failure.isPresent()) {
                return failure.get();
            }

            sb.append("<").append(args.stream().map(TypeString::value).collect(Collectors.joining(", "))).append(">");
        }

        return TypeString.successful(sb.toString(), getClass(), TypeString.Target.READABLE);
    }

    @Override
    public void lock() {
        this.arguments = List.copyOf(this.arguments);

        this.argMap = new LinkedHashMap<>();
        this.parentMap = new LinkedHashMap<>();
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeParameterized(this.typeSystem(), this.declaration,
                this.arguments.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
    }

    @Override
    public void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
            this.arguments.forEach(t -> t.forEach(consumer, seen));
        }
    }

    @Override
    public boolean isProperType() {
        return this.arguments.stream().allMatch(TypeConcrete::isProperType);
    }

    @Override
    public Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen) {
        Set<TypeConcrete> res = new HashSet<>();
        this.arguments.stream().map(t -> t.circularChildren(seen, this)).forEach(res::addAll);
        return res;
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeParameterized(this.typeSystem(), this.declaration, this.arguments.stream().map(TypeConcrete::flatten).collect(Collectors.toList()));
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeParameterized typeParameterized = (TypeParameterized) o;

        if (!Objects.equals(declaration, typeParameterized.declaration))
            return false;
        return Objects.equals(arguments, typeParameterized.arguments);
    }

    @Override
    public int hashCodeExactly() {
        int result = declaration != null ? declaration.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }
}
