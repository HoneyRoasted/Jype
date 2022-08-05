package honeyroasted.jype.system;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.cache.SimpleTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.resolution.ReflectionTypeResolver;
import honeyroasted.jype.system.resolution.SequentialTypeResolutionStrategy;
import honeyroasted.jype.system.resolution.TypeMirrorTypeResolver;
import honeyroasted.jype.system.resolution.TypeResolutionStrategy;
import honeyroasted.jype.system.resolution.TypeTokenTypeResolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.erasure.ErasureConstraint;
import honeyroasted.jype.system.solver.erasure.ErasureTypeSolver;
import honeyroasted.jype.system.solver.force.ForceResolveTypeSolver;
import honeyroasted.jype.type.TypeArray;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeNone;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypePrimitive;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
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
    public final TypeClass VOID_BOX;

    /**
     * The {@code boolean} primitive type.
     */
    public final TypePrimitive BOOLEAN;
    /**
     * The boxing type for {@link TypeSystem#BOOLEAN}
     */
    public final TypeClass BOOLEAN_BOX;

    /**
     * The {@code byte} primitive type.
     */
    public final TypePrimitive BYTE;
    /**
     * The boxing type for {@link TypeSystem#BYTE}.
     */
    public final TypeClass BYTE_BOX;

    /**
     * The {@code short} primitive type.
     */
    public final TypePrimitive SHORT;
    /**
     * The boxing type for {@link TypeSystem#SHORT}.
     */
    public final TypeClass SHORT_BOX;

    /**
     * The {@code char} primitive type.
     */
    public final TypePrimitive CHAR;
    /**
     * The boxing type for {@link TypeSystem#CHAR}
     */
    public final TypeClass CHAR_BOX;

    /**
     * The {@code int} primitive type.
     */
    public final TypePrimitive INT;
    /**
     * The boxing type for {@link TypeSystem#INT}
     */
    public final TypeClass INT_BOX;

    /**
     * The {@code long} primitive type.
     */
    public final TypePrimitive LONG;
    /**
     * The boxing type for {@link TypeSystem#LONG}.
     */
    public final TypeClass LONG_BOX;

    /**
     * The {@code float} primitive type.
     */
    public final TypePrimitive FLOAT;
    /**
     * The boxing type for {@link TypeSystem#FLOAT}.
     */
    public final TypeClass FLOAT_BOX;

    /**
     * The {@code double} primitive type.
     */
    public final TypePrimitive DOUBLE;
    /**
     * The boxing type for {@link TypeSystem#DOUBLE}.
     */
    public final TypeClass DOUBLE_BOX;

    /**
     * The type corresponding to an instantiation of {@link Object}.
     */
    public final TypeClass OBJECT;
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

    private TypeResolutionStrategy strategy;

    public TypeSystem() {
        this(null);
    }

    public TypeSystem(TypeResolutionStrategy strategy) {
        if (strategy == null) {
            this.strategy = new SequentialTypeResolutionStrategy()
                    .add(new ReflectionTypeResolver(this, new SimpleTypeCache<>()))
                    .add(new TypeMirrorTypeResolver(this, new SimpleTypeCache<>()))
                    .add(new TypeTokenTypeResolver(this, new SimpleTypeCache<>()));
        } else {
            this.strategy = strategy;
        }

        NONE = new TypeNone(this, "none", "V");
        NULL = new TypeNull(this);

        VOID = new TypeNone(this, "void", "V");
        BOOLEAN = new TypePrimitive(this, boolean.class, "Z");
        BYTE = new TypePrimitive(this, byte.class, "B");
        SHORT = new TypePrimitive(this, short.class, "S");
        CHAR = new TypePrimitive(this, char.class, "C");
        INT = new TypePrimitive(this, int.class, "I");
        LONG = new TypePrimitive(this, long.class, "J");
        FLOAT = new TypePrimitive(this, float.class, "F");
        DOUBLE = new TypePrimitive(this, double.class, "D");

        VOID_BOX = of(Void.class);
        BOOLEAN_BOX = of(Boolean.class);
        BYTE_BOX = of(Byte.class);
        SHORT_BOX = of(Short.class);
        CHAR_BOX = of(Character.class);
        INT_BOX = of(Integer.class);
        LONG_BOX = of(Long.class);
        FLOAT_BOX = of(Float.class);
        DOUBLE_BOX = of(Double.class);
        OBJECT = of(Object.class);
        OBJECT_CLASS = declaration(Object.class);

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

        BOX_TO_PRIM = reverse(PRIM_TO_BOX, Namespace::of);
    }

    public TypeParameter newParameter(String name) {
        return new TypeParameter(this, name);
    }

    public TypeDeclaration newDeclaration(Namespace namespace, boolean isInterface) {
        return new TypeDeclaration(this, namespace, isInterface);
    }

    public TypeClass newType(TypeDeclaration declaration) {
        return new TypeClass(this, declaration);
    }

    public TypeArray newArray(TypeConcrete element) {
        return new TypeArray(this, element);
    }

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

    public boolean isAssignableTo(TypeConcrete left, TypeConcrete right) {
        return new ForceResolveTypeSolver(this)
                .constrain(new TypeConstraint.Bound(left, right))
                .solve()
                .successful();
    }

    public Optional<TypeConcrete> erasure(TypeConcrete type) {
        TypeSolution solution = new ErasureTypeSolver(this)
                .constrain(new ErasureConstraint.Erasure(type))
                .solve();

        return solution.successful() ? solution.context().get(type) : Optional.empty();
    }

    public <T extends TypeConcrete> T of(Object type) {
        return (T) this.strategy.resolve(type);
    }

    public TypeDeclaration declaration(Object type) {
        return this.strategy.resolveDeclaration(type);
    }

    public Optional<TypePrimitive> unbox(TypeConcrete type) {
        return type instanceof TypeClass cls ? Optional.ofNullable(BOX_TO_PRIM.get(cls.declaration().namespace())) :
                Optional.empty();
    }

    public TypeConcrete box(TypePrimitive type) {
        return of(PRIM_TO_BOX.get(type));
    }

    private static <K, V, T> Map<T, V> reverse(Map<V, K> map, Function<K, T> keyFunction) {
        Map<T, V> result = new HashMap<>();
        map.forEach((key, val) -> result.put(keyFunction.apply(val), key));
        return Map.copyOf(result);
    }
}
