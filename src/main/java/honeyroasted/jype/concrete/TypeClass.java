package honeyroasted.jype.concrete;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.declaration.TypeDeclaration;
import honeyroasted.jype.declaration.TypeParameter;
import honeyroasted.jype.system.Constraint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeClass implements TypeConcrete {
    private TypeDeclaration declaration;
    private List<TypeConcrete> arguments = new ArrayList<>();

    private Map<TypeParameter, TypeConcrete> argMap = new LinkedHashMap<>();
    private Map<TypeDeclaration, TypeClass> parentMap = new LinkedHashMap<>();

    public TypeClass(TypeDeclaration declaration) {
        this.declaration = declaration;
    }

    public TypeClass(TypeDeclaration declaration, List<TypeConcrete> arguments) {
        this.declaration = declaration;
        this.arguments = arguments;
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
        return this.declaration.pathTo(parent).flatMap(path -> {
            TypeClass current = this;
            for (int i = 1; i < path.size(); i++) {
                Optional<TypeClass> next = current.directParent(path.get(i));
                if (next.isPresent()) {
                    current = next.get();
                } else {
                    return Optional.empty();
                }
            }

            return Optional.of(current);
        });
    }

    public Optional<TypeClass> directParent(TypeDeclaration parent) {
        return this.declaration.directParent(parent).map(typeParent -> {
            TypeClass result = new TypeClass(parent);
            for (TypeConcrete arg : typeParent.arguments()) {
                result.arguments().add(arg.map(t -> {
                    if (t instanceof TypeParameterReference ref && this.argument(ref.variable()).isPresent()) {
                        return this.argument(ref.variable()).get();
                    }
                    return t;
                }));
            }

            result.lock();
            return result;
        });
    }

    @Override
    public void lock() {
        this.arguments = List.copyOf(this.arguments);
        this.arguments.forEach(Type::lock);

        this.argMap = new LinkedHashMap<>();
        this.parentMap = new LinkedHashMap<>();
    }

    @Override
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeClass(this.declaration.map(mapper),
                this.arguments.stream().map(t -> (TypeConcrete) t.map(mapper)).toList()));
    }

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        if (other instanceof TypePrimitive prim) {
            Optional<TypePrimitive> unbox = TypePrimitive.unbox(this.declaration.namespace());
            if (unbox.isPresent()) {
                return unbox.get().assignabilityTo(prim);
            }
        } else if (other instanceof TypeClass otherClass) {
            TypeClass self = this;

            if (!self.declaration().equals(otherClass.declaration())) {
                Optional<TypeClass> parent = self.parent(otherClass.declaration());
                if (parent.isPresent()) {
                    self = parent.get();
                } else {
                    return Constraint.FALSE;
                }
            }

            if (self.arguments().isEmpty() || otherClass.arguments().isEmpty()) {
                return Constraint.TRUE; //Unchecked conversion
            } else if (self.arguments().size() != otherClass.arguments().size()) {
                return Constraint.FALSE;
            } else {
                List<Constraint> constraints = new ArrayList<>();
                for (int i = 0; i < self.arguments().size(); i++) {
                    TypeConcrete ti = self.arguments().get(i);
                    TypeConcrete si = otherClass.arguments().get(i);

                    if (si instanceof TypeOut typeOut) { //? extends X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        constraints.add(ti.assignabilityTo(bound)
                                .and(ti.assignabilityTo(typeOut.bound())));
                    } else if (si instanceof TypeIn typeIn) { //? super X
                        TypeConcrete bound = otherClass.declaration().parameters().get(i)
                                .bound().resolveVariables(t -> otherClass.argument(t).get());
                        constraints.add(ti.assignabilityTo(bound)
                                .and(typeIn.bound().assignabilityTo(ti)));
                    } else {
                        constraints.add(new Constraint.Equal(ti, si));
                    }
                }
                return new Constraint.And(constraints);
            }
        }

        return TypeConcrete.defaultTests(this, other, Constraint.FALSE);
    }

    @Override
    public String toString() {
        return this.declaration.namespace() +
                (this.arguments.isEmpty() ? "" :
                        "<" + this.arguments.stream().map(TypeConcrete::toString).collect(Collectors.joining(", ")) + ">");
    }
}
