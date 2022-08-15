package honeyroasted.jype.system;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.cache.SimpleTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.resolution.ReflectionTypeResolver;
import honeyroasted.jype.system.resolution.SequentialTypeResolver;
import honeyroasted.jype.system.resolution.TypeMirrorTypeResolver;
import honeyroasted.jype.system.resolution.TypeResolver;
import honeyroasted.jype.system.resolution.TypeTokenTypeResolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.erasure.ErasureConstraint;
import honeyroasted.jype.system.solver.erasure.ErasureTypeSolver;
import honeyroasted.jype.system.solver.force.ForceResolveTypeSolver;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;
import honeyroasted.jype.type.TypePrimitive;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * This class represents a type system, along with a {@link TypeCache}. It allows creation of types from reflection
 * values and {@link TypeMirror} values (for annotation processing). Additionally, it provides standard types such
 * as {@link TypeSystem#NULL}, all the primitives, their boxes, and the {@link TypeSystem#OBJECT} class.
 */
public class TypeSystem {
    /**
     * The global type system, for when constructing your own {@link TypeSystem} is unnecessary
     */
    public static final TypeSystem GLOBAL = new TypeSystem();

    /**
     * The NONE type. Equivalent in bytecode to {@link TypeSystem#VOID}, but indicates that a type may be unknown or
     * nonexistent.
     */
    public final TypeNone NONE;
    /**
     * The NULL type. Assignable to any reference type, only the expression {@code null} can have this type.
     */
    public final TypeNull NULL;

    /**
     * The VOID type. Equivalent in bytecode to {@link TypeSystem#NONE}.
     */
    public final TypeNone VOID;
    /**
     * The boxing type for {@link TypeSystem#VOID}.
     */
    public final TypeParameterized VOID_BOX;

    /**
     * The {@code boolean} primitive type.
     */
    public final TypePrimitive BOOLEAN;
    /**
     * The boxing type for {@link TypeSystem#BOOLEAN}
     */
    public final TypeParameterized BOOLEAN_BOX;

    /**
     * The {@code byte} primitive type.
     */
    public final TypePrimitive BYTE;
    /**
     * The boxing type for {@link TypeSystem#BYTE}.
     */
    public final TypeParameterized BYTE_BOX;

    /**
     * The {@code short} primitive type.
     */
    public final TypePrimitive SHORT;
    /**
     * The boxing type for {@link TypeSystem#SHORT}.
     */
    public final TypeParameterized SHORT_BOX;

    /**
     * The {@code char} primitive type.
     */
    public final TypePrimitive CHAR;
    /**
     * The boxing type for {@link TypeSystem#CHAR}
     */
    public final TypeParameterized CHAR_BOX;

    /**
     * The {@code int} primitive type.
     */
    public final TypePrimitive INT;
    /**
     * The boxing type for {@link TypeSystem#INT}
     */
    public final TypeParameterized INT_BOX;

    /**
     * The {@code long} primitive type.
     */
    public final TypePrimitive LONG;
    /**
     * The boxing type for {@link TypeSystem#LONG}.
     */
    public final TypeParameterized LONG_BOX;

    /**
     * The {@code float} primitive type.
     */
    public final TypePrimitive FLOAT;
    /**
     * The boxing type for {@link TypeSystem#FLOAT}.
     */
    public final TypeParameterized FLOAT_BOX;

    /**
     * The {@code double} primitive type.
     */
    public final TypePrimitive DOUBLE;
    /**
     * The boxing type for {@link TypeSystem#DOUBLE}.
     */
    public final TypeParameterized DOUBLE_BOX;

    /**
     * The type corresponding to an instantiation of {@link Object}.
     */
    public final TypeParameterized OBJECT;
    /**
     * The type corresponding to the class declaration of {@link Object}.
     */
    public final TypeDeclaration OBJECT_CLASS;

    /**
     * The set of all primitive types (not including {@code void}).
     */
    public final Set<TypePrimitive> ALL_PRIMITIVES;

    private final Map<TypePrimitive, Class<?>> PRIM_TO_BOX;
    private final Map<Namespace, TypePrimitive> BOX_TO_PRIM;

    private TypeResolver<Object, Object> strategy;
    private TypeCache<TypeConcrete> cache;

    /**
     * Creates a new {@link TypeSystem} with the default {@link TypeResolver}.
     */
    public TypeSystem() {
        this(new SimpleTypeCache<>(), t -> {
            TypeCache<Object> shared = new SimpleTypeCache<>();

            return new SequentialTypeResolver(List.of(
                    new ReflectionTypeResolver(t, shared),
                    new TypeMirrorTypeResolver(t, shared),
                    new TypeTokenTypeResolver(t, shared)));
        });
    }

    /**
     * Creates a new {@link TypeSystem} with the given {@link TypeResolver}.
     *
     * @param cache The {@link TypeCache} used by this {@link TypeSystem} for removing circular references
     * @param strategy The {@link TypeResolver} to use
     */
    public TypeSystem(TypeCache<TypeConcrete> cache, Function<TypeSystem, TypeResolver<?, ?>> strategy) {
        this.strategy = (TypeResolver<Object, Object>) strategy.apply(this);
        this.cache = cache;

        NONE = new TypeNone(this, "none");
        NULL = new TypeNull(this);

        VOID = new TypeNone(this, "void");
        BOOLEAN = new TypePrimitive(this, boolean.class, "Z");
        BYTE = new TypePrimitive(this, byte.class, "B");
        SHORT = new TypePrimitive(this, short.class, "S");
        CHAR = new TypePrimitive(this, char.class, "C");
        INT = new TypePrimitive(this, int.class, "I");
        LONG = new TypePrimitive(this, long.class, "J");
        FLOAT = new TypePrimitive(this, float.class, "F");
        DOUBLE = new TypePrimitive(this, double.class, "D");

        VOID_BOX = (TypeParameterized) of(Void.class).get();
        BOOLEAN_BOX = (TypeParameterized) of(Boolean.class).get();
        BYTE_BOX = (TypeParameterized) of(Byte.class).get();
        SHORT_BOX = (TypeParameterized) of(Short.class).get();
        CHAR_BOX = (TypeParameterized) of(Character.class).get();
        INT_BOX = (TypeParameterized) of(Integer.class).get();
        LONG_BOX = (TypeParameterized) of(Long.class).get();
        FLOAT_BOX = (TypeParameterized) of(Float.class).get();
        DOUBLE_BOX = (TypeParameterized) of(Double.class).get();
        OBJECT = (TypeParameterized) of(Object.class).get();
        OBJECT_CLASS = declaration(Object.class).get();

        ALL_PRIMITIVES = Set.of(BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE);

        PRIM_TO_BOX = Map.of(
                BOOLEAN, Boolean.class,
                BYTE, Byte.class,
                SHORT, Short.class,
                CHAR, Character.class,
                INT, Integer.class,
                LONG, Long.class,
                FLOAT, Float.class,
                DOUBLE, Double.class
        );

        Map<Namespace, TypePrimitive> boxToPrim = new HashMap<>();
        PRIM_TO_BOX.forEach((prim, cls) -> boxToPrim.put(Namespace.of(cls), prim));

        BOX_TO_PRIM = Map.copyOf(boxToPrim);
    }

    /**
     * Creates a new {@link TypeParameter} with the given name. It will be mutable until
     * {@link Type#lock()} is called.
     *
     * @param name The name
     * @return A new {@link TypeParameter}
     */
    public TypeParameter newParameter(String name) {
        return new TypeParameter(this, name);
    }

    /**
     * Creates a new {@link TypeDeclaration} with the given {@link Namespace} and interface status.
     * It will be mutable until {@link Type#lock()} is called.
     *
     * @param namespace   The {@link Namespace}
     * @param isInterface Whether the new {@link TypeDeclaration} is an interface
     * @return A mew {@link TypeDeclaration}
     */
    public TypeDeclaration newDeclaration(Namespace namespace, boolean isInterface) {
        return new TypeDeclaration(this, namespace, isInterface);
    }

    /**
     * Creates a new {@link TypeParameterized} with the given {@link TypeDeclaration}.
     * It will be mutable until {@link Type#lock()} is called.
     *
     * @param declaration The {@link TypeDeclaration}
     * @return A new {@link TypeParameterized}
     */
    public TypeParameterized newType(TypeDeclaration declaration) {
        return new TypeParameterized(this, declaration);
    }

    /**
     * Creates a new {@link TypeArray} with the given element. It will be mutable until {@link Type#lock()} is called.
     *
     * @param element The array element
     * @return A new {@link TypeArray}
     */
    public TypeArray newArray(TypeConcrete element) {
        return new TypeArray(this, element);
    }

    /**
     * Creates a new {@link TypeArray} with the given element, and the given dimensions.
     * It will be mutable until {@link Type#lock()} is called. This may produce a {@link TypeArray}
     * with dimensions greater than {@code dims} if {@code element} is a {@link TypeArray}.
     *
     * @param element The array element
     * @param dims    The dimensions of the new array
     * @return A new {@link TypeArray}
     * @throws IllegalArgumentException if {@code dims < 1}
     */
    public TypeArray newArray(TypeConcrete element, int dims) {
        if (dims < 1) {
            throw new IllegalArgumentException("Array must have at least 1 dimension");
        }

        TypeArray res = new TypeArray(this, element);
        for (int i = 0; i < dims - 1; i++) {
            res = new TypeArray(this, res);
        }
        return res;
    }

    /**
     * This method tests if one {@link TypeConcrete} is assignable to another. It performs no type
     * inference, and does not support circular type references (e.g. List&lt;T&gt; where T extends T). Any circular
     * {@link TypeParameter}s, {@link TypeIn}s, or {@link TypeOut}s will be replaced with the wildcard type
     * {@code ? extends Object}. In general, if this method returns true, it is guaranteed that {@code left} is
     * assignable to {@code right}. However, if this method returns false, there may be some set of type parameter
     * substitutions that would make it true.
     *
     * @param left  The type to check if it can be assigned to {@code right}
     * @param right The type to check if {@code left} can be assigned to it
     * @return true if {@code left} can be assigned to {@code right}
     */
    public boolean isAssignableTo(TypeConcrete left, TypeConcrete right) {
        return new ForceResolveTypeSolver(this)
                .constrain(new TypeConstraint.Bound(this.deCircularize(left), this.deCircularize(right)))
                .solve()
                .successful();
    }

    /**
     * This is a utility method to 'de-circularize' a {@link TypeConcrete}. That is, it replaces any {@link TypeParameter}s,
     * {@link TypeIn}s, or {@link TypeOut}s that have circular bounds with a wildcard type of the form {@code ? extends Object}.
     * This is useful for inspecting types through a method that does not support circular type references.
     *
     * @param type The type to remove circular references from
     * @return A type without circular references
     */
    public TypeConcrete deCircularize(TypeConcrete type) {
        type.circularChildren().forEach(t -> {
            if (!this.cache.has(t, TypeConcrete.class)) {
                this.cache.cache(t, new TypeOut(this, this.OBJECT), TypeConcrete.class);
            }
        });

        return type.map(t -> {
            if (this.cache.has(t, TypeConcrete.class)) {
                return this.cache.get(t, TypeConcrete.class);
            } else {
                return t;
            }
        });
    }

    /**
     * Utilizes the {@link ErasureTypeSolver} to produce the {@link TypeConcrete} of the given type. The Java compiler erases
     * certain generic type information to comply with backwards compatibility. This method will perform the same
     * process on the given {@link TypeConcrete}.
     *
     * @param type The {@link TypeConcrete} to apply type erasure to
     * @return An {@link Optional} containing the erased {@link TypeConcrete}, or empty if erasure could not be performed
     */
    public Optional<TypeConcrete> erasure(TypeConcrete type) {
        TypeSolution solution = new ErasureTypeSolver(this)
                .constrain(new ErasureConstraint.Erasure(type))
                .solve();

        return solution.successful() ? solution.context().get(type) : Optional.empty();
    }

    /**
     * Attempts to resolve a {@link TypeConcrete} from the given object.
     *
     * @param type The object to resolve to a {@link TypeConcrete}
     * @param <T>  Type parameter to facilitate loose typing
     * @return An {@link Optional} containing the resolved {@link TypeConcrete}, or empty if it could not be resolved
     * @throws ClassCastException if {@code T} is not the type of the result
     */
    public <T extends TypeConcrete> Optional<T> of(Object type) {
        return Optional.ofNullable((T) this.strategy.resolve(type));
    }

    /**
     * Attempts to resolve a {@link TypeDeclaration} from the given object.
     *
     * @param type The object to resolve to a {@link TypeDeclaration}
     * @return An {@link Optional} containing the resolved {@link TypeDeclaration}, or empty if it could not be resolved
     */
    public Optional<TypeDeclaration> declaration(Object type) {
        return Optional.ofNullable(this.strategy.resolveDeclaration(type));
    }

    /**
     * This is a utility method to unbox a {@link TypeConcrete} to a {@link TypePrimitive}.
     *
     * @param type The {@link TypeConcrete} to unbox
     * @return An {@link Optional} contain the unboxed {@link TypePrimitive}, or an empty {@link Optional} if
     * {@code type} is not a one of the primitive boxing types
     */
    public Optional<TypePrimitive> unbox(TypeConcrete type) {
        return type instanceof TypeParameterized cls ? Optional.ofNullable(BOX_TO_PRIM.get(cls.declaration().namespace())) :
                Optional.empty();
    }

    /**
     * This is a utility method to box a {@link TypePrimitive} into a {@link TypeConcrete}.
     *
     * @param type The {@link TypePrimitive} to box
     * @return The boxing {@link TypeConcrete} for {@code type}
     */
    public TypeConcrete box(TypePrimitive type) {
        return of(PRIM_TO_BOX.get(type)).get();
    }

}
