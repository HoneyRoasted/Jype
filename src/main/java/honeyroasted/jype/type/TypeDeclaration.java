package honeyroasted.jype.type;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class represents a declared type. That is, the actual declaration of a class or interface. It contains information
 * about the declaration's name, type parameters, and parents. Instantiations of objects are represented by {@link TypeParameterized}.
 * Every {@link TypeParameterized} has a corresponding {@link TypeDeclaration}, but a {@link TypeDeclaration} may have infinite
 * possible {@link TypeParameterized}s.
 */
public class TypeDeclaration implements Type {
    private TypeSystem typeSystem;
    private Namespace namespace;
    private List<TypeParameter> parameters;
    private List<TypeParameterized> parents;
    private boolean isInterface;

    /**
     * Creates a new {@link TypeDeclaration}.
     *
     * @param system      The {@link TypeSystem} this {@link TypeDeclaration} is a member of
     * @param namespace   The {@link Namespace} representing the name of this {@link TypeDeclaration}
     * @param parameters  The type parameters of this {@link TypeDeclaration}
     * @param parents     The parents (whether superclass, or interfaces) of this {@link TypeDeclaration}
     * @param isInterface Whether this {@link TypeDeclaration} represents an interface declaration
     */
    public TypeDeclaration(TypeSystem system, Namespace namespace, List<TypeParameter> parameters, List<TypeParameterized> parents, boolean isInterface) {
        this.typeSystem = system;
        this.namespace = namespace;
        this.parameters = parameters;
        this.parents = parents;
        this.isInterface = isInterface;
    }

    /**
     * Creates a new {@link TypeDeclaration} with no parents and no type parameters.
     *
     * @param system      The {@link TypeSystem} this {@link TypeDeclaration} is a member of
     * @param namespace   The {@link Namespace} representing the name of this {@link TypeDeclaration}
     * @param isInterface Whether this {@link TypeDeclaration} represents an interface declaration
     */
    public TypeDeclaration(TypeSystem system, Namespace namespace, boolean isInterface) {
        this(system, namespace, new ArrayList<>(), new ArrayList<>(), isInterface);
    }

    /**
     * Returns the inheritance path of {@link TypeDeclaration}s needed to reach the given parent type. For example, if
     * this {@link TypeDeclaration} represents the type {@code Integer}, the inheritance path necessary to reach
     * {@code Object} is:
     * <ul>
     * <li>{@code Integer}</li>
     * <li>{@code Number}</li>
     * <li>{@code Object}</li>
     * </ul>
     *
     * @param parent The parent {@link TypeDeclaration} to construct an inheritance path to
     * @return An optional containing the inheritance path, or an empty optional if one could not be found
     */
    public Optional<List<TypeDeclaration>> pathTo(TypeDeclaration parent) {
        List<TypeDeclaration> path = new ArrayList<>();
        path.add(this);

        if (this.parents.stream().anyMatch(t -> t.declaration().equals(parent))) {
            path.add(parent);
        } else {
            for (TypeParameterized type : this.parents) {
                Optional<List<TypeDeclaration>> pathOpt = type.declaration().pathTo(parent);
                if (pathOpt.isPresent()) {
                    path.addAll(pathOpt.get());
                    break;
                }
            }
        }

        return path.get(path.size() - 1).equals(parent) ?
                Optional.of(path) : Optional.empty();
    }

    /**
     * Constructs a {@link TypeParameterized} from this {@link TypeDeclaration} and the given {@link TypeConcrete} arguments.
     *
     * @param arguments The arguments for the new {@link TypeParameterized}
     * @return A new {@link TypeParameterized}
     * @throws IllegalArgumentException If the number of arguments is not 0 or equal to the number of parameters in this
     *                                  {@link TypeDeclaration}
     */
    public TypeParameterized withArgList(List<TypeConcrete> arguments) {
        if (arguments.isEmpty() || arguments.size() == this.parameters.size()) {
            TypeParameterized clazz = new TypeParameterized(this.typeSystem(), this, arguments);
            clazz.lock();
            return clazz;
        }

        throw new IllegalArgumentException("Expected 0 arguments or " + this.parameters.size() + " argument(s)");
    }

    /**
     * Constructs a {@link TypeParameterized} from this {@link TypeDeclaration} and the given {@link TypeConcrete} arguments.
     *
     * @param arguments The arguments for the new {@link TypeParameterized}
     * @return A new {@link TypeParameterized}
     * @throws IllegalArgumentException If the number of arguments is not 0 or equal to the number of parameters in this
     *                                  {@link TypeDeclaration}
     */
    public TypeParameterized withArguments(TypeConcrete... arguments) {
        return withArgList(Arrays.asList(arguments));
    }

    /**
     * @return The {@link Namespace} of this {@link TypeDeclaration}
     */
    public Namespace namespace() {
        return this.namespace;
    }

    /**
     * @return The type parameters of this {@link TypeDeclaration}
     */
    public List<TypeParameter> parameters() {
        return this.parameters;
    }

    /**
     * @return The parents of this {@link TypeDeclaration}, including the superclass and interfaces
     */
    public List<TypeParameterized> parents() {
        return this.parents;
    }

    /**
     * @return The fully qualified name of this {@link TypeDeclaration}
     */
    public String name() {
        return this.namespace.name();
    }

    /**
     * @return The java virtual machine internal name of this {@link TypeDeclaration}
     */
    public String internalName() {
        return this.namespace.internalName();
    }

    /**
     * @return True if this {@link TypeDeclaration} represents an interface, false otherwise
     */
    public boolean isInterface() {
        return this.isInterface;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            StringBuilder sb = new StringBuilder().append("L" + this.namespace.internalName());
            if (!this.parameters.isEmpty()) {
                sb.append("<");
                List<TypeString> args = this.parameters.stream().map(t -> t.toSignature(context)).toList();
                Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                if (failure.isPresent()) {
                    return failure.get();
                }
                sb.append(args.stream().map(TypeString::value).collect(Collectors.joining())).append(">");
            }
            return TypeString.successful(sb.append(";").toString(), getClass(), TypeString.Target.SIGNATURE);
        } else {
            StringBuilder sb = new StringBuilder().append("L" + this.namespace.internalName());
            if (!this.parameters.isEmpty()) {
                sb.append("<");
                List<TypeString> args = this.parameters.stream().map(t -> t.toSignature(context)).toList();
                Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                if (failure.isPresent()) {
                    return failure.get();
                }
                sb.append(args.stream().map(TypeString::value).collect(Collectors.joining())).append(">");
            }

            List<TypeString> args = this.parents.stream().filter(t -> (!this.isInterface || !t.equals(this.typeSystem.OBJECT))).map(t -> t.toSignature(context)).toList();
            Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
            if (failure.isPresent()) {
                return failure.get();
            }
            sb.append(args.stream().map(TypeString::value).collect(Collectors.joining()));

            return TypeString.successful(sb.toString(), getClass(), TypeString.Target.SIGNATURE);
        }
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful("L" + this.namespace.internalName() + ";", getClass(), TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.namespace().name(), getClass(), TypeString.Target.SOURCE);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.namespace().name());

            if (!this.parameters.isEmpty()) {
                sb.append("<");
                List<TypeString> args = this.parameters.stream().map(t -> t.toSignature(context)).toList();
                Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                if (failure.isPresent()) {
                    return failure.get();
                }
                sb.append(args.stream().map(TypeString::value).collect(Collectors.joining(", "))).append(">");
            }

            if (!this.parents.isEmpty() && !this.parents.stream().allMatch(t -> t.equals(this.typeSystem.OBJECT))) {
                if (!this.parents.get(0).equals(this.typeSystem.OBJECT)) {
                    sb.append(" extends ");

                    TypeString superclass = this.parents.get(0).toSource(TypeString.Context.CONCRETE);
                    if (!superclass.successful()) {
                        return superclass;
                    }

                    sb.append(superclass.value());
                }

                if (this.parents.size() > 1) {
                    sb.append(this.isInterface ? "extends" : " implements ");
                    List<TypeString> args = this.parents.subList(1, this.parameters.size()).stream().map(t -> t.toSignature(TypeString.Context.CONCRETE)).toList();
                    Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                    if (failure.isPresent()) {
                        return failure.get();
                    }
                    sb.append(args.stream().map(TypeString::value).collect(Collectors.joining(", ")));
                }
            }

            return TypeString.successful(sb.toString(), getClass(), TypeString.Target.SOURCE);
        }
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        if (context == TypeString.Context.CONCRETE) {
            return TypeString.successful(this.namespace().simpleName(), getClass(), TypeString.Target.READABLE);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.namespace().name());

            if (!this.parameters.isEmpty()) {
                sb.append("<");
                List<TypeString> args = this.parameters.stream().map(t -> t.toReadable(context)).toList();
                Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                if (failure.isPresent()) {
                    return failure.get();
                }
                sb.append(args.stream().map(TypeString::value).collect(Collectors.joining(", "))).append(">");
            }

            if (!this.parents.isEmpty() && !this.parents.stream().allMatch(t -> t.equals(this.typeSystem().OBJECT))) {
                if (!this.parents.get(0).equals(this.typeSystem().OBJECT)) {
                    sb.append(" extends ");

                    TypeString superclass = this.parents.get(0).toSource(TypeString.Context.CONCRETE);
                    if (!superclass.successful()) {
                        return superclass;
                    }

                    sb.append(superclass.value());
                }

                if (this.parents.size() > 1) {
                    sb.append(this.isInterface ? "extends" : " implements ");
                    List<TypeString> args = this.parents.subList(1, this.parameters.size()).stream().map(t -> t.toReadable(TypeString.Context.CONCRETE)).toList();
                    Optional<TypeString> failure = args.stream().filter(t -> !t.successful()).findFirst();
                    if (failure.isPresent()) {
                        return failure.get();
                    }
                    sb.append(args.stream().map(TypeString::value).collect(Collectors.joining(", ")));
                }
            }

            return TypeString.successful(sb.toString(), getClass(), TypeString.Target.READABLE);
        }
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public void lock() {
        this.parameters = List.copyOf(this.parameters);
        this.parents = List.copyOf(this.parents);
    }

    @Override
    public String toString() {
        return this.namespace.name() + (this.parameters.isEmpty() ? "" :
                "<" + this.parameters.stream().map(Type::toString).collect(Collectors.joining(", ")) + ">") +
                (this.parents.isEmpty() ? "" :
                        " extends " + this.parents.stream().map(TypeConcrete::toString).collect(Collectors.joining(", ")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeDeclaration that = (TypeDeclaration) o;
        return Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return this.namespace.hashCode();
    }
}
