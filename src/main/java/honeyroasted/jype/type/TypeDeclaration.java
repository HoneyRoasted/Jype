package honeyroasted.jype.type;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeDeclaration implements Type {
    private Namespace namespace;
    private List<TypeParameter> parameters;
    private List<TypeClass> parents;

    private Map<TypeDeclaration, TypeClass> parentMap = new LinkedHashMap<>();

    public TypeDeclaration(Namespace namespace, List<TypeParameter> parameters, List<TypeClass> parents) {
        this.namespace = namespace;
        this.parameters = parameters;
        this.parents = parents;
    }

    public TypeDeclaration(Namespace namespace) {
        this(namespace, new ArrayList<>(), new ArrayList<>());
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

    @Override
    public void lock() {
        this.parameters = List.copyOf(this.parameters);
        this.parents = List.copyOf(this.parents);

        this.parents.forEach(Type::lock);
        this.parameters.forEach(Type::lock);

        this.parentMap = new LinkedHashMap<>();
    }

    public TypeClass withArgList(List<TypeConcrete> arguments) {
        if (arguments.isEmpty() || arguments.size() == this.parameters.size()) {
            TypeClass clazz = new TypeClass(this, arguments);
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

        if (!Objects.equals(namespace, that.namespace)) return false;
        if (!Objects.equals(parameters, that.parameters)) return false;
        return Objects.equals(parents, that.parents);
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (parents != null ? parents.hashCode() : 0);
        return result;
    }
}
