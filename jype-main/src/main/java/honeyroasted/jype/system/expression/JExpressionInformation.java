package honeyroasted.jype.system.expression;

import honeyroasted.almonds.SimpleName;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JFieldLookup;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.JOperatorTypes;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JInstantiableType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface JExpressionInformation extends SimpleName {

    JExpressionInformation toSimple(JType type);

    static SimplyTyped of(JType type) {
        return new SimplyTyped.Simple(type);
    }

    default boolean isSimplyTyped() {
        return false;
    }

    default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
        return Optional.empty();
    }

    interface SimplyTyped extends JExpressionInformation {
        record Simple(JType type) implements SimplyTyped {
            @Override
            public JType type(JTypeSystem system) {
                return this.type;
            }

            @Override
            public String simpleName() {
                return this.type.simpleName();
            }

            @Override
            public String toString() {
                return "simply-typed(" + this.type + ")";
            }
        }

        default SimplyTyped toSimple(JType type) {
            return new SimplyTyped.Simple(type);
        }

        default SimplyTyped createNew(JType type) { return new SimplyTyped.Simple(type); }

        JType type(JTypeSystem system);

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(this.type(system)));
        }
    }

    interface Constant extends SimplyTyped {
        record Simple(Object value) implements Constant {
            @Override
            public String simpleName() {
                return this.value instanceof String str ? "\"" + escape(str) + "\"" :
                        String.valueOf(this.value);
            }

            @Override
            public String toString() {
                return "const(" + this.value + ")";
            }

            private static char esc = '\\';
            private static Map<Character, String> escapes = Map.of(
                    '0', "\0",
                    'b', "\b",
                    't', "\t",
                    'n', "\n",
                    'f', "\f",
                    'r', "\r",
                    '"', "\"",
                    '\'', "'",
                    '\\', "\\");
            private static String escape(String string) {
                if (string == null) {
                    return "";
                }

                if(escapes.entrySet().stream().anyMatch(c -> c.getKey() == esc && c.getValue().equals(String.valueOf(esc)))) {
                    string = string.replace(String.valueOf(esc), esc + "" + esc);
                }

                for (Map.Entry<Character, String> entry : escapes.entrySet()) {
                    if (entry.getKey() != esc || !entry.getValue().equals(String.valueOf(esc))) {
                        String re = esc + "" + entry.getKey();
                        string = string.replace(entry.getValue(), re);
                    }
                }

                return string;
            }

        }

        Object value();

        default Constant createNew(Object value) { return new Constant.Simple(value); }

        @Override
        default JType type(JTypeSystem system) {
            if (value() == null) {
                return system.constants().nullType();
            } else {
                JType type = system.tryResolve(value().getClass());
                if (type instanceof JClassType ct) {
                    JType unboxed = system.constants().primitiveByBox().get(ct.namespace());
                    if (unboxed != null) {
                        return unboxed;
                    }
                }
                return type;
            }
        }
    }

    interface Multi extends JExpressionInformation {
        record Simple(List<? extends JExpressionInformation> children) implements Multi {
            @Override
            public String simpleName() {
                return this.children().stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(" & "));
            }

            @Override
            public String toString() {
                return "multi(" + this.children.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ")";
            }
        }

        record Typed(Multi delegate, JType simpleType) implements Multi {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public List<? extends JExpressionInformation> children() { return delegate.children(); }
        }

        List<? extends JExpressionInformation> children();

        @Override
        default Multi toSimple(JType type) {
            return new Multi.Typed(this, type);
        }

        default Multi createNew(List<? extends JExpressionInformation> children) { return new Multi.Simple(children); }

        @Override
        default boolean isSimplyTyped() {
            return this.children().stream().allMatch(JExpressionInformation::isSimplyTyped);
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            if (!this.isSimplyTyped()) {
                return Optional.empty();
            }

            if (this.children().isEmpty()) {
                return Optional.of(system.constants().voidType());
            } else if (this.children().size() == 1) {
                return this.children().get(0).getSimpleType(system, Function.identity());
            } else {
                Set<JType> childrenTypes = new LinkedHashSet<>();
                this.children().forEach(c -> {
                    JType childType = c.getSimpleType(system, Function.identity()).get();

                    if (childType instanceof JIntersectionType it) {
                        childrenTypes.addAll(it.children());
                    } else {
                        childrenTypes.add(childType);
                    }
                });

                return Optional.of(mapper.apply(JIntersectionType.of(childrenTypes, system)));
            }
        }
    }


    interface Invocation extends JExpressionInformation {
        JClassReference declaring();

        JMethodReference declaringMethod();

        List<JExpressionInformation> parameters();

        List<JArgumentType> explicitTypeArguments();
    }

    interface Instantiation extends Invocation {
        record Simple(JClassReference declaring, JMethodReference declaringMethod, JClassReference type, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements Instantiation {
            @Override
            public String simpleName() {
                return "new " + this.type.simpleName() +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
            }

            @Override
            public String toString() {
                return "instantiation(" + this.type +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::toString).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ") IN " + (this.declaringMethod == null ? this.declaring : this.declaringMethod) + ")";
            }
        }

        record Typed(Instantiation delegate, JType simpleType) implements Instantiation {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JClassReference declaring() { return delegate.declaring(); }
            @Override public JMethodReference declaringMethod() { return delegate.declaringMethod(); }
            @Override public List<JExpressionInformation> parameters() { return delegate.parameters(); }
            @Override public List<JArgumentType> explicitTypeArguments() { return delegate.explicitTypeArguments(); }
            @Override public JClassReference type() { return delegate.type(); }
        }

        @Override
        default Instantiation toSimple(JType type) {
            return new Instantiation.Typed(this, type);
        }

        default Instantiation createNew(JClassReference declaring, JMethodReference declaringMethod, JClassReference type, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) { return new Instantiation.Simple(declaring, declaringMethod, type, parameters, explicitTypeArguments); }

        JClassReference type();
    }

    /**
     * Marks a method-invocation source as a (possibly qualified) super-invocation, e.g.
     * {@code super.method()} (where {@code outer} is the invoking class itself) or
     * {@code Outer.super.method()} (where {@code outer} is a lexically enclosing class).
     * Method resolution for such an invocation begins at {@code outer}'s direct superclass, so
     * overrides declared in {@code outer} itself are skipped, matching the non-virtual dispatch
     * semantics of JLS §15.12.1.
     */
    record SuperReference(JClassReference outer) implements SimpleName {
        @Override
        public String simpleName() {
            return this.outer.simpleName() + ".super";
        }

        @Override
        public String toString() {
            return this.outer + ".super";
        }
    }

    interface MethodInvocation<T extends SimpleName> extends Invocation {
        record Simple<T extends SimpleName>(JClassReference declaring, JMethodReference declaringMethod, T source, String name, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements MethodInvocation<T> {
            @Override
            public String simpleName() {
                return this.source.simpleName() + "." + this.name +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
            }

            @Override
            public String toString() {
                return "invocation(" + this.source + "." + this.name +
                        (this.explicitTypeArguments.isEmpty() ? "" : "<" + this.explicitTypeArguments.stream().map(JType::toString).collect(Collectors.joining(", ")) + ">") +
                        "(" + this.parameters.stream().map(JExpressionInformation::toString).collect(Collectors.joining(", ")) + ") IN " + (this.declaringMethod == null ? this.declaring : this.declaringMethod) + ")";
            }
        }

        record Typed<T extends SimpleName>(MethodInvocation<T> delegate, JType simpleType) implements MethodInvocation<T> {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JClassReference declaring() { return delegate.declaring(); }
            @Override public JMethodReference declaringMethod() { return delegate.declaringMethod(); }
            @Override public List<JExpressionInformation> parameters() { return delegate.parameters(); }
            @Override public List<JArgumentType> explicitTypeArguments() { return delegate.explicitTypeArguments(); }
            @Override public T source() { return delegate.source(); }
            @Override public String name() { return delegate.name(); }
        }

        @Override
        default MethodInvocation<T> toSimple(JType type) {
            return new MethodInvocation.Typed<>(this, type);
        }

        default MethodInvocation<T> createNew(JClassReference declaring, JMethodReference declaringMethod, T source, String name, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) { return new MethodInvocation.Simple<>(declaring, declaringMethod, source, name, parameters, explicitTypeArguments); }

        T source();

        String name();
    }

    interface GetField<T extends SimpleName> extends JExpressionInformation {
        record Simple<T extends SimpleName>(JClassReference declaring, JMethodReference declaringMethod, T source, String name) implements GetField<T> {
            @Override
            public String simpleName() {
                return this.source.simpleName() + "." + this.name;
            }

            @Override
            public String toString() {
                return "getfield(" + this.source + "." + this.name + ") IN " +
                        (this.declaringMethod == null ? this.declaring : this.declaringMethod);
            }
        }


        @Override
        default boolean isSimplyTyped() {
            JTypeSystem system = this.declaring() != null ? this.declaring().typeSystem() :
                    this.source() instanceof JClassReference ref ? ref.typeSystem() : null;
            return system != null && this.getSimpleType(system, Function.identity()).isPresent();
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            if (this.source() instanceof JClassReference jcr) {
                return JFieldLookup.staticAccess(jcr, this.name(), this.declaring()).map(JFieldLookup.Found::type).map(mapper);
            } else if (this.source() instanceof JExpressionInformation expr && expr.isSimplyTyped()) {
                Optional<JType> receiver = expr.getSimpleType(system, mapper);
                if (receiver.isEmpty()) {
                    return Optional.empty();
                }
                return JFieldLookup.instanceAccess(receiver.get(), this.name(), this.declaring()).map(JFieldLookup.Found::type).map(mapper);
            }
            return Optional.empty();
        }

        record Typed<T extends SimpleName>(GetField<T> delegate, JType simpleType) implements GetField<T> {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JClassReference declaring() { return delegate.declaring(); }
            @Override public JMethodReference declaringMethod() { return delegate.declaringMethod(); }
            @Override public T source() { return delegate.source(); }
            @Override public String name() { return delegate.name(); }
        }

        @Override
        default GetField<T> toSimple(JType type) {
            return new GetField.Typed<>(this, type);
        }

        default GetField<T> createNew(JClassReference declaring, JMethodReference declaringMethod, T source, String name) { return new GetField.Simple<>(declaring, declaringMethod, source, name); }

        JClassReference declaring();

        JMethodReference declaringMethod();

        T source();

        String name();
    }

    interface Lambda extends JExpressionInformation {
        record Simple(JExpressionInformation body, List<JType> explicitParameterTypes, int parameterCount, boolean implicitReturn) implements Lambda {
            @Override
            public String simpleName() {
                return "(" + (this.explicitParameterTypes != null && this.explicitParameterTypes.size() == parameterCount ?
                        this.explicitParameterTypes.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) :
                        IntStream.range(0, parameterCount).mapToObj(i -> "arg" + i).collect(Collectors.joining(", "))) +
                        ") -> " + (this.implicitReturn ? "" : "{ ") + this.body.simpleName() + (this.implicitReturn ? "" : " }");
            }

            @Override
            public String toString() {
                return "lambda([" + (this.explicitParameterTypes != null && this.explicitParameterTypes.size() == parameterCount ?
                        this.explicitParameterTypes.stream().map(Object::toString).collect(Collectors.joining(", ")) :
                        this.parameterCount) + "] -> "
                        + (this.implicitReturn ? "" : "{ ") + this.body + (this.implicitReturn ? "" : " }");
            }
        }

        record Typed(Lambda delegate, JType simpleType) implements Lambda {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JExpressionInformation body() { return delegate.body(); }
            @Override public List<JType> explicitParameterTypes() { return delegate.explicitParameterTypes(); }
            @Override public int parameterCount() { return delegate.parameterCount(); }
            @Override public boolean implicitReturn() { return delegate.implicitReturn(); }
        }

        @Override
        default Lambda toSimple(JType type) {
            return new Lambda.Typed(this, type);
        }

        default Lambda createNew(JExpressionInformation body, List<JType> explicitParameterTypes, int parameterCount, boolean implicitReturn) { return new Lambda.Simple(body, explicitParameterTypes, parameterCount, implicitReturn); }

        JExpressionInformation body();

        List<JType> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InstantiationReference extends JExpressionInformation {
        record Simple(JInstantiableType type, List<JArgumentType> explicitTypeArguments) implements InstantiationReference {
            @Override
            public String simpleName() {
                return type.simpleName() + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "::new";
            }

            @Override
            public String toString() {
                return "instantiation_ref(" + this.type + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ">") +
                        ")";
            }
        }

        record Typed(InstantiationReference delegate, JType simpleType) implements InstantiationReference {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JInstantiableType type() { return delegate.type(); }
            @Override public List<JArgumentType> explicitTypeArguments() { return delegate.explicitTypeArguments(); }
        }

        @Override
        default InstantiationReference toSimple(JType type) {
            return new InstantiationReference.Typed(this, type);
        }

        default InstantiationReference createNew(JInstantiableType type, List<JArgumentType> explicitTypeArguments) { return new InstantiationReference.Simple(type, explicitTypeArguments); }

        JInstantiableType type();

        List<JArgumentType> explicitTypeArguments();
    }

    interface InvocationReference<T extends SimpleName> extends JExpressionInformation {
        record Simple<T extends SimpleName>(T source, String methodName, List<JArgumentType> explicitTypeArguments) implements InvocationReference<T> {
            @Override
            public String simpleName() {
                return source.simpleName() + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(SimpleName::simpleName).collect(Collectors.joining(", ")) + ">") +
                        "::" + methodName;
            }

            @Override
            public String toString() {
                return "instantiation_ref(" + this.source + (this.explicitTypeArguments == null || this.explicitTypeArguments.isEmpty() ? "" :
                        "<" + this.explicitTypeArguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ">") +
                        ":: " + methodName + ")";
            }
        }

        record Typed<T extends SimpleName>(InvocationReference<T> delegate, JType simpleType) implements InvocationReference<T> {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public T source() { return delegate.source(); }
            @Override public String methodName() { return delegate.methodName(); }
            @Override public List<JArgumentType> explicitTypeArguments() { return delegate.explicitTypeArguments(); }
        }

        @Override
        default InvocationReference<T> toSimple(JType type) {
            return new InvocationReference.Typed<>(this, type);
        }

        default InvocationReference<T> createNew(T source, String methodName, List<JArgumentType> explicitTypeArguments) { return new InvocationReference.Simple<>(source, methodName, explicitTypeArguments); }

        T source();

        String methodName();

        List<JArgumentType> explicitTypeArguments();
    }

    /**
     * An explicit cast expression {@code (T) operand} (JLS §15.16/§5.5). Its own type is
     * {@code castType} unconditionally; whether the cast itself is legal is checked separately
     * (see {@link honeyroasted.jype.system.solver.constraints.reduction.JReduceCast}).
     */
    interface Cast extends JExpressionInformation {
        record Simple(JExpressionInformation operand, JType castType) implements Cast {
            @Override
            public String simpleName() {
                return "(" + this.castType.simpleName() + ") " + this.operand.simpleName();
            }

            @Override
            public String toString() {
                return "cast(" + this.castType + ", " + this.operand + ")";
            }
        }

        JExpressionInformation operand();

        JType castType();

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(this.castType()));
        }

        @Override
        default JExpressionInformation toSimple(JType type) {
            return this;
        }

        default Cast createNew(JExpressionInformation operand, JType castType) {
            return new Cast.Simple(operand, castType);
        }
    }

    /**
     * A type comparison expression {@code operand instanceof T} (JLS §15.20.2). Its own type is
     * always {@code boolean}; whether the comparison is legal at all is checked via the same
     * castability rule as an explicit cast (JLS §15.20.2 explicitly defers to §5.5's casting
     * conversion rules here) - see {@link honeyroasted.jype.system.solver.constraints.reduction.JReduceInstanceOf}.
     * Pattern variable bindings ({@code instanceof T name}) are a flow-scoping concern this
     * expression-level model does not represent; only the boolean type-comparison itself is checked.
     */
    interface InstanceOf extends JExpressionInformation {
        record Simple(JExpressionInformation operand, JType checkedType) implements InstanceOf {
            @Override
            public String simpleName() {
                return this.operand.simpleName() + " instanceof " + this.checkedType.simpleName();
            }

            @Override
            public String toString() {
                return "instanceof(" + this.operand + ", " + this.checkedType + ")";
            }
        }

        JExpressionInformation operand();

        JType checkedType();

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(system.constants().booleanType()));
        }

        @Override
        default JExpressionInformation toSimple(JType type) {
            return this;
        }

        default InstanceOf createNew(JExpressionInformation operand, JType checkedType) {
            return new InstanceOf.Simple(operand, checkedType);
        }
    }

    /**
     * A unary operator expression (JLS §15.15): {@code +}/{@code -} (numeric, unary numeric
     * promotion applied per §5.6.1), {@code ~} (bitwise complement, integral operand only, unary
     * numeric promotion applied), or {@code !} (logical complement, boolean operand only). Like
     * {@link MethodInvocation} or {@link Instantiation}, this is never simply typed on its own:
     * whether it's legal - and thus what its type is - depends on the operand's type, which is
     * only known once solved, so {@code isSimplyTyped()==true} could never be a safe promise here
     * (that contract requires {@code getSimpleType()} to always be present, and there's no
     * operand-type-independent way to guarantee that without a {@link JTypeSystem} in hand, which
     * {@code isSimplyTyped()} doesn't receive). {@link
     * honeyroasted.jype.system.solver.constraints.reduction.JReduceUnaryOperator} resolves the
     * operand's type (directly if it's simply typed, otherwise via its own sub-solve, exactly like
     * a nested method invocation would be) and applies {@link #resultType} to it.
     * {@code ++}/{@code --} are deliberately not modeled: they require an assignable-lvalue notion
     * this expression-only model doesn't have.
     */
    interface UnaryOperator extends JExpressionInformation {
        enum Kind {
            PLUS("+"), MINUS("-"), BITWISE_COMPLEMENT("~"), LOGICAL_COMPLEMENT("!");

            private final String symbol;

            Kind(String symbol) {
                this.symbol = symbol;
            }

            public String symbol() {
                return this.symbol;
            }
        }

        record Simple(Kind kind, JExpressionInformation operand) implements UnaryOperator {
            @Override
            public String simpleName() {
                return this.kind.symbol() + this.operand.simpleName();
            }

            @Override
            public String toString() {
                return "unary(" + this.kind.symbol() + ", " + this.operand + ")";
            }
        }

        record Typed(UnaryOperator delegate, JType simpleType) implements UnaryOperator {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public Kind kind() { return delegate.kind(); }
            @Override public JExpressionInformation operand() { return delegate.operand(); }
        }

        Kind kind();

        JExpressionInformation operand();

        default UnaryOperator createNew(Kind kind, JExpressionInformation operand) {
            return new UnaryOperator.Simple(kind, operand);
        }

        @Override
        default UnaryOperator toSimple(JType type) {
            return new UnaryOperator.Typed(this, type);
        }

        static Optional<JType> resultType(Kind kind, JType operandType, JTypeSystem system) {
            return switch (kind) {
                case PLUS, MINUS -> JOperatorTypes.unaryNumericPromotion(operandType, system).map(t -> t);
                case BITWISE_COMPLEMENT -> JOperatorTypes.unaryNumericPromotion(operandType, system)
                        .filter(t -> JOperatorTypes.isIntegral(t, system)).map(t -> t);
                case LOGICAL_COMPLEMENT -> JOperatorTypes.asPrimitive(operandType, system)
                        .filter(t -> JOperatorTypes.isBoolean(t, system)).map(t -> system.constants().booleanType());
            };
        }
    }

    /**
     * A binary operator expression (JLS §15.17-§15.24): arithmetic ({@code + - * / %}, with
     * {@code +} also covering string concatenation per §15.18.1 when either operand's type is
     * exactly {@code String}), shift ({@code << >> >>>}, each operand independently unary-promoted,
     * result type is the promoted left operand type per §15.19), relational ({@code < > <= >=}),
     * equality ({@code == !=}, numeric/boolean per usual promotion or - for two reference types -
     * requiring one side castable to the other per §15.21), bitwise/logical ({@code & ^ |}, either
     * both-boolean or both-integral), and conditional ({@code && ||}, both-boolean). As with
     * {@link UnaryOperator}, this is never simply typed on its own - see that type's docs for why -
     * and {@link honeyroasted.jype.system.solver.constraints.reduction.JReduceBinaryOperator}
     * resolves both operands' types (directly where possible, otherwise via their own sub-solve)
     * before applying {@link #resultType}.
     */
    interface BinaryOperator extends JExpressionInformation {
        enum Kind {
            ADD, SUB, MUL, DIV, MOD,
            SHL, SHR, USHR,
            LT, GT, LE, GE,
            EQ, NE,
            AND, OR, XOR,
            COND_AND, COND_OR
        }

        record Simple(Kind kind, JExpressionInformation left, JExpressionInformation right) implements BinaryOperator {
            @Override
            public String simpleName() {
                return this.left.simpleName() + " " + symbol(this.kind) + " " + this.right.simpleName();
            }

            @Override
            public String toString() {
                return "binary(" + symbol(this.kind) + ", " + this.left + ", " + this.right + ")";
            }
        }

        static String symbol(Kind kind) {
            return switch (kind) {
                case ADD -> "+";
                case SUB -> "-";
                case MUL -> "*";
                case DIV -> "/";
                case MOD -> "%";
                case SHL -> "<<";
                case SHR -> ">>";
                case USHR -> ">>>";
                case LT -> "<";
                case GT -> ">";
                case LE -> "<=";
                case GE -> ">=";
                case EQ -> "==";
                case NE -> "!=";
                case AND -> "&";
                case OR -> "|";
                case XOR -> "^";
                case COND_AND -> "&&";
                case COND_OR -> "||";
            };
        }

        record Typed(BinaryOperator delegate, JType simpleType) implements BinaryOperator {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public Kind kind() { return delegate.kind(); }
            @Override public JExpressionInformation left() { return delegate.left(); }
            @Override public JExpressionInformation right() { return delegate.right(); }
        }

        Kind kind();

        JExpressionInformation left();

        JExpressionInformation right();

        default BinaryOperator createNew(Kind kind, JExpressionInformation left, JExpressionInformation right) {
            return new BinaryOperator.Simple(kind, left, right);
        }

        @Override
        default BinaryOperator toSimple(JType type) {
            return new BinaryOperator.Typed(this, type);
        }

        static boolean isExactlyString(JType type, JTypeSystem system) {
            JClassReference string = system.tryResolve(String.class);
            return string != null && type.typeEquals(string);
        }

        static Optional<JType> resultType(Kind kind, JType leftType, JType rightType, JTypeSystem system) {
            switch (kind) {
                case ADD: {
                    if (isExactlyString(leftType, system) || isExactlyString(rightType, system)) {
                        JClassReference string = system.tryResolve(String.class);
                        return Optional.of(string);
                    }
                    return JOperatorTypes.binaryNumericPromotion(leftType, rightType, system).map(t -> t);
                }
                case SUB:
                case MUL:
                case DIV:
                case MOD:
                    return JOperatorTypes.binaryNumericPromotion(leftType, rightType, system).map(t -> t);
                case SHL:
                case SHR:
                case USHR: {
                    Optional<JPrimitiveType> l = JOperatorTypes.unaryNumericPromotion(leftType, system)
                            .filter(t -> JOperatorTypes.isIntegral(t, system));
                    Optional<JPrimitiveType> r = JOperatorTypes.unaryNumericPromotion(rightType, system)
                            .filter(t -> JOperatorTypes.isIntegral(t, system));
                    if (l.isEmpty() || r.isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(l.get());
                }
                case LT:
                case GT:
                case LE:
                case GE:
                    return JOperatorTypes.binaryNumericPromotion(leftType, rightType, system).isPresent() ?
                            Optional.of(system.constants().booleanType()) : Optional.empty();
                case EQ:
                case NE: {
                    Optional<JPrimitiveType> l = JOperatorTypes.asPrimitive(leftType, system);
                    Optional<JPrimitiveType> r = JOperatorTypes.asPrimitive(rightType, system);
                    boolean bothBoolean = l.isPresent() && r.isPresent() &&
                            JOperatorTypes.isBoolean(l.get(), system) && JOperatorTypes.isBoolean(r.get(), system);
                    boolean bothNumeric = JOperatorTypes.binaryNumericPromotion(leftType, rightType, system).isPresent();
                    if (bothBoolean || bothNumeric) {
                        return Optional.of(system.constants().booleanType());
                    } else if (l.isEmpty() && r.isEmpty()) {
                        boolean castable = system.operations().isCompatible(leftType, rightType, JTypeConstraints.Compatible.Context.EXPLICIT_CAST) ||
                                system.operations().isCompatible(rightType, leftType, JTypeConstraints.Compatible.Context.EXPLICIT_CAST);
                        return castable ? Optional.of(system.constants().booleanType()) : Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                }
                case AND:
                case OR:
                case XOR: {
                    Optional<JPrimitiveType> l = JOperatorTypes.asPrimitive(leftType, system);
                    Optional<JPrimitiveType> r = JOperatorTypes.asPrimitive(rightType, system);
                    boolean bothBoolean = l.isPresent() && r.isPresent() &&
                            JOperatorTypes.isBoolean(l.get(), system) && JOperatorTypes.isBoolean(r.get(), system);
                    if (bothBoolean) {
                        return Optional.of(system.constants().booleanType());
                    }
                    return JOperatorTypes.binaryIntegralPromotion(leftType, rightType, system).map(t -> t);
                }
                case COND_AND:
                case COND_OR: {
                    Optional<JPrimitiveType> l = JOperatorTypes.asPrimitive(leftType, system);
                    Optional<JPrimitiveType> r = JOperatorTypes.asPrimitive(rightType, system);
                    boolean bothBoolean = l.isPresent() && r.isPresent() &&
                            JOperatorTypes.isBoolean(l.get(), system) && JOperatorTypes.isBoolean(r.get(), system);
                    return bothBoolean ? Optional.of(system.constants().booleanType()) : Optional.empty();
                }
                default:
                    return Optional.empty();
            }
        }
    }

    /**
     * A conditional expression {@code condition ? whenTrue : whenFalse} (JLS §15.25). Like
     * {@link UnaryOperator}/{@link BinaryOperator}, never simply typed on its own - the merge rule
     * below needs the branches' resolved types in hand. {@link
     * honeyroasted.jype.system.solver.constraints.reduction.JReduceConditionalExpression} handles
     * two distinct cases: if either branch is an inherently poly expression form (a lambda or a
     * method/constructor reference, which cannot be typed without a target type at all), the outer
     * context is propagated directly to both branches per JLS's own "poly conditional expression"
     * rule; otherwise both branches are resolved to concrete types (directly, or via their own
     * sub-solve) and merged with {@link #resultType}.
     */
    interface Conditional extends JExpressionInformation {
        record Simple(JExpressionInformation condition, JExpressionInformation whenTrue,
                       JExpressionInformation whenFalse) implements Conditional {
            @Override
            public String simpleName() {
                return this.condition.simpleName() + " ? " + this.whenTrue.simpleName() + " : " + this.whenFalse.simpleName();
            }

            @Override
            public String toString() {
                return "conditional(" + this.condition + ", " + this.whenTrue + ", " + this.whenFalse + ")";
            }
        }

        record Typed(Conditional delegate, JType simpleType) implements Conditional {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public JExpressionInformation condition() { return delegate.condition(); }
            @Override public JExpressionInformation whenTrue() { return delegate.whenTrue(); }
            @Override public JExpressionInformation whenFalse() { return delegate.whenFalse(); }
        }

        JExpressionInformation condition();

        JExpressionInformation whenTrue();

        JExpressionInformation whenFalse();

        default Conditional createNew(JExpressionInformation condition, JExpressionInformation whenTrue, JExpressionInformation whenFalse) {
            return new Conditional.Simple(condition, whenTrue, whenFalse);
        }

        @Override
        default Conditional toSimple(JType type) {
            return new Conditional.Typed(this, type);
        }

        /**
         * JLS §15.25's numeric/boolean/reference merge for a "standalone" (non-poly) conditional
         * expression, given the two branches' already-resolved types. {@code trueExpr}/{@code
         * falseExpr} are needed alongside the resolved types only for the constant-narrowing rule
         * (a byte/short/char operand paired with a representable {@code int} constant on the other
         * side keeps the narrower type).
         */
        static Optional<JType> resultType(JExpressionInformation trueExpr, JType trueType, JExpressionInformation falseExpr, JType falseType, JTypeSystem system) {
            if (trueType.typeEquals(falseType)) {
                return Optional.of(trueType);
            }

            var c = system.constants();
            Optional<JPrimitiveType> lp = JOperatorTypes.asPrimitive(trueType, system);
            Optional<JPrimitiveType> rp = JOperatorTypes.asPrimitive(falseType, system);

            if (lp.isPresent() && rp.isPresent()) {
                JPrimitiveType l = lp.get();
                JPrimitiveType r = rp.get();
                boolean lBool = JOperatorTypes.isBoolean(l, system);
                boolean rBool = JOperatorTypes.isBoolean(r, system);

                if (lBool != rBool) {
                    return Optional.empty();
                } else if (lBool) {
                    return Optional.of(c.booleanType());
                }

                if ((l.typeEquals(c.byteType()) && r.typeEquals(c.shortType())) || (l.typeEquals(c.shortType()) && r.typeEquals(c.byteType()))) {
                    return Optional.of(c.shortType());
                }

                Optional<JType> narrowed = narrowConstant(trueExpr, l, r, system)
                        .or(() -> narrowConstant(falseExpr, r, l, system));
                if (narrowed.isPresent()) {
                    return narrowed;
                }

                return JOperatorTypes.binaryNumericPromotion(trueType, falseType, system).map(t -> t);
            }

            JType lref = lp.map(p -> (JType) p.box()).orElse(trueType);
            JType rref = rp.map(p -> (JType) p.box()).orElse(falseType);
            return Optional.of(system.operations().findLeastUpperBound(new LinkedHashSet<>(List.of(lref, rref))));
        }

        private static Optional<JType> narrowConstant(JExpressionInformation constCandidate, JPrimitiveType constCandidatePrim, JPrimitiveType otherPrim, JTypeSystem system) {
            var c = system.constants();
            if (!constCandidatePrim.typeEquals(c.intType()) || !(constCandidate instanceof Constant constant) || !(constant.value() instanceof Integer i)) {
                return Optional.empty();
            }

            long v = i;
            if (otherPrim.typeEquals(c.byteType())) {
                return (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) ? Optional.of(otherPrim) : Optional.empty();
            } else if (otherPrim.typeEquals(c.shortType())) {
                return (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) ? Optional.of(otherPrim) : Optional.empty();
            } else if (otherPrim.typeEquals(c.charType())) {
                return (v >= Character.MIN_VALUE && v <= Character.MAX_VALUE) ? Optional.of(otherPrim) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    /**
     * An array-creation expression (JLS §15.10): either dimension-expression form
     * ({@code new T[e1][e2]...}, optionally with trailing dimensionless brackets like
     * {@code new T[e1][]}), or initializer form ({@code new T[]{...}}, {@code initializer} holding
     * the top-level {@link ArrayInitializer}). {@code dimensions.size() + extraDimensions} is the
     * array's total depth. Unlike {@link UnaryOperator}/{@link BinaryOperator}/{@link Conditional},
     * the resulting type here never depends on operand legality - it's exactly
     * {@code componentType} wrapped in that many array dimensions regardless of whether the
     * dimension expressions or initializer elements are themselves legal - so, like {@link Cast},
     * this is always simply typed; {@link
     * honeyroasted.jype.system.solver.constraints.reduction.JReduceArrayCreation} checks dimension-
     * expression and initializer legality as independent side constraints.
     */
    interface ArrayCreation extends JExpressionInformation {
        record Simple(JType componentType, List<JExpressionInformation> dimensions, int extraDimensions,
                       JExpressionInformation initializer) implements ArrayCreation {
            @Override
            public String simpleName() {
                StringBuilder sb = new StringBuilder("new ").append(this.componentType.simpleName());
                this.dimensions.forEach(d -> sb.append('[').append(d.simpleName()).append(']'));
                for (int i = 0; i < this.extraDimensions; i++) {
                    sb.append("[]");
                }
                if (this.initializer != null) {
                    sb.append(' ').append(this.initializer.simpleName());
                }
                return sb.toString();
            }

            @Override
            public String toString() {
                return "array_creation(" + this.componentType + ", dims=" + this.dimensions + ", extraDims=" + this.extraDimensions +
                        (this.initializer == null ? "" : ", init=" + this.initializer) + ")";
            }
        }

        JType componentType();

        List<JExpressionInformation> dimensions();

        int extraDimensions();

        JExpressionInformation initializer();

        default ArrayCreation createNew(JType componentType, List<JExpressionInformation> dimensions, int extraDimensions, JExpressionInformation initializer) {
            return new ArrayCreation.Simple(componentType, dimensions, extraDimensions, initializer);
        }

        default JType arrayType(JTypeSystem system) {
            JType result = this.componentType();
            int totalDimensions = this.dimensions().size() + this.extraDimensions();
            for (int i = 0; i < totalDimensions; i++) {
                JArrayType arrayType = system.typeFactory().newArrayType();
                arrayType.setComponent(result);
                result = arrayType;
            }
            return result;
        }

        @Override
        default boolean isSimplyTyped() {
            return true;
        }

        @Override
        default Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) {
            return Optional.of(mapper.apply(this.arrayType(system)));
        }

        @Override
        default JExpressionInformation toSimple(JType type) {
            return this;
        }

        default ArrayCreation createNew(JType componentType, List<JExpressionInformation> dimensions, int extraDimensions) {
            return new ArrayCreation.Simple(componentType, dimensions, extraDimensions, null);
        }
    }

    /**
     * A bare array initializer {@code {e1, e2, ...}} (JLS §10.6), only ever meaningful nested
     * inside an {@link ArrayCreation} or another {@code ArrayInitializer} - it has no type of its
     * own without an externally supplied target array type to determine the component type each
     * element is checked against, so - like {@link Lambda} - it's never simply typed.
     * {@link honeyroasted.jype.system.solver.constraints.reduction.JReduceArrayInitializer} reads
     * that target type from its own enclosing {@code ExpressionCompatible} constraint and checks
     * each element against the component type (recursing naturally for nested initializers, since
     * a nested element that's itself an {@code ArrayInitializer} goes through the same reducer
     * again with the next-level-down component type as its target).
     */
    interface ArrayInitializer extends JExpressionInformation {
        record Simple(List<JExpressionInformation> elements) implements ArrayInitializer {
            @Override
            public String simpleName() {
                return "{" + this.elements.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + "}";
            }

            @Override
            public String toString() {
                return "array_initializer(" + this.elements + ")";
            }
        }

        record Typed(ArrayInitializer delegate, JType simpleType) implements ArrayInitializer {
            @Override public boolean isSimplyTyped() { return true; }
            @Override public Optional<JType> getSimpleType(JTypeSystem system, Function<JType, JType> mapper) { return Optional.of(mapper.apply(simpleType)); }
            @Override public String simpleName() { return delegate.simpleName(); }
            @Override public List<JExpressionInformation> elements() { return delegate.elements(); }
        }

        List<JExpressionInformation> elements();

        default ArrayInitializer createNew(List<JExpressionInformation> elements) {
            return new ArrayInitializer.Simple(elements);
        }

        @Override
        default ArrayInitializer toSimple(JType type) {
            return new ArrayInitializer.Typed(this, type);
        }
    }

}
