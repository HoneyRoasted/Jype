package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeClass extends AbstractType implements TypeConcrete {
    private TypeDeclaration declaration;
    private List<TypeConcrete> arguments;

    private Map<TypeParameter, TypeConcrete> argMap = new LinkedHashMap<>();
    private Map<TypeDeclaration, TypeClass> parentMap = new LinkedHashMap<>();

    public TypeClass(TypeSystem system, TypeDeclaration declaration, List<TypeConcrete> arguments) {
        super(system);
        this.declaration = declaration;
        this.arguments = arguments;
    }

    public TypeClass(TypeSystem system, TypeDeclaration declaration) {
        this(system, declaration, new ArrayList<>());
    }


    public TypeDeclaration declaration() {
        return this.declaration;
    }

    public List<TypeConcrete> arguments() {
        return this.arguments;
    }

    public Optional<TypeConcrete> argument(TypeParameter parameter) {
        return Optional.ofNullable(this.argMap.computeIfAbsent(parameter, key ->
                this.declaration.parameters().stream().filter(t -> t.equals(key)).findFirst().map(tp -> {
                    int index = this.declaration.parameters().indexOf(tp);
                    return index >= 0 && index < this.arguments.size() ? this.arguments.get(index) :
                            null;
                }).orElse(null)));
    }

    public Optional<TypeClass> parent(TypeDeclaration parent) {
        return Optional.ofNullable(this.parentMap.computeIfAbsent(parent, key -> this.declaration.pathTo(parent).map(path -> {
            TypeClass current = this;
            for (int i = 1; i < path.size(); i++) {
                Optional<TypeClass> next = current.directParent(path.get(i));
                if (next.isPresent()) {
                    current = next.get();
                } else {
                    return null;
                }
            }

            return current;
        }).orElse(null)));
    }

    public Optional<TypeClass> directParent(TypeDeclaration parent) {
        return this.declaration.directParent(parent).map(typeParent -> {
            TypeClass result = new TypeClass(this.typeSystem(), parent);
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
        return (T) mapper.apply(new TypeClass(this.typeSystem(), this.declaration,
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
    public TypeConcrete flatten() {
        return new TypeClass(this.typeSystem(), this.declaration, this.arguments.stream().map(TypeConcrete::flatten).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return this.declaration.namespace() +
                (this.arguments.isEmpty() ? "" :
                        "<" + this.arguments.stream().map(TypeConcrete::toString).collect(Collectors.joining(", ")) + ">");
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeClass typeClass = (TypeClass) o;

        if (!Objects.equals(declaration, typeClass.declaration))
            return false;
        return Objects.equals(arguments, typeClass.arguments);
    }

    @Override
    public int hashCodeExactly() {
        int result = declaration != null ? declaration.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }
}
