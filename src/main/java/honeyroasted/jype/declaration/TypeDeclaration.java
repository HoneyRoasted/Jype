package honeyroasted.jype.declaration;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypeClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeDeclaration(this.namespace, this.parameters.stream().map(t -> (TypeParameter) t.map(mapper)).toList(),
                this.parents.stream().map(t -> (TypeClass) t.map(mapper)).toList()));
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

}
