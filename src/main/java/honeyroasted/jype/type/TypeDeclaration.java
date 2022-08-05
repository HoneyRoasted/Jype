package honeyroasted.jype.type;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeDeclaration implements Type {
    private TypeSystem typeSystem;
    private Namespace namespace;
    private List<TypeParameter> parameters;
    private List<TypeClass> parents;
    private boolean isInterface;

    private Map<TypeDeclaration, TypeClass> parentMap = new LinkedHashMap<>();

    public TypeDeclaration(TypeSystem system, Namespace namespace, List<TypeParameter> parameters, List<TypeClass> parents, boolean isInterface) {
        this.typeSystem = system;
        this.namespace = namespace;
        this.parameters = parameters;
        this.parents = parents;
        this.isInterface = isInterface;
    }

    public TypeDeclaration(TypeSystem system, Namespace namespace, boolean isInterface) {
        this(system, namespace, new ArrayList<>(), new ArrayList<>(), isInterface);
    }

    public Optional<TypeClass> directParent(TypeDeclaration parent) {
        return Optional.ofNullable(this.parentMap.computeIfAbsent(parent, key ->
                this.parents.stream().filter(t -> t.declaration().equals(key)).findFirst().orElse(null)));
    }

    public Optional<List<TypeDeclaration>> pathTo(TypeDeclaration parent) {
        List<TypeDeclaration> path = new ArrayList<>();
        path.add(this);

        if (this.parents.stream().anyMatch(t -> t.declaration().equals(parent))) {
            path.add(parent);
        } else {
            for (TypeClass type : this.parents) {
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

    public TypeClass withArgList(List<TypeConcrete> arguments) {
        if (arguments.isEmpty() || arguments.size() == this.parameters.size()) {
            TypeClass clazz = new TypeClass(this.typeSystem(), this, arguments);
            clazz.lock();
            return clazz;
        }

        throw new IllegalArgumentException("Expected 0 arguments or " + this.parameters.size() + " argument(s)");
    }

    public TypeClass withArguments(TypeConcrete... arguments) {
        return withArgList(Arrays.asList(arguments));
    }

    public Namespace namespace() {
        return this.namespace;
    }

    public List<TypeParameter> parameters() {
        return this.parameters;
    }

    public List<TypeClass> parents() {
        return this.parents;
    }

    public String name() {
        return this.namespace.name();
    }

    public String internalName() {
        return this.namespace.internalName();
    }

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

        this.parentMap = new LinkedHashMap<>();
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
