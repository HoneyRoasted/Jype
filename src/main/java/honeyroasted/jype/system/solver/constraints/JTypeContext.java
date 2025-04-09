package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.SimpleName;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface JTypeContext {

    record ChosenMethod(JExpressionInformation.Invocation expression, JMethodReference chosen, JTypeConstraints.Compatible.Context context, boolean vararg) implements SimpleName {
        @Override
        public String simpleName() {
            return "Chosen Method: " + chosen.simpleName() + " in " + context + " for expression: " + expression.simpleName();
        }
    }

    final class TypeMetavarMap implements Function<JType, JType>, SimpleName {
        private final Map<JMetaVarType, JType> instantiations;
        private final Map<JVarType, JMetaVarType> metaVars;

        private JVarTypeResolveVisitor varTypeResolveVisitor;
        private JMetaVarTypeResolveVisitor metaVarTypeResolveVisitor;

        public TypeMetavarMap(Map<JMetaVarType, JType> instantiations, Map<JVarType, JMetaVarType> metaVars) {
            this.instantiations = instantiations;
            this.metaVars = metaVars;

            this.varTypeResolveVisitor = new JVarTypeResolveVisitor(this.metaVars);
            this.metaVarTypeResolveVisitor = new JMetaVarTypeResolveVisitor(this.instantiations);
        }

        public static TypeMetavarMap createEmpty() {
            return new TypeMetavarMap(new HashMap<>(), new HashMap<>());
        }

        private static final TypeMetavarMap empty = new TypeMetavarMap(Collections.emptyMap(), Collections.emptyMap());
        public static TypeMetavarMap empty() {
            return empty;
        }

        public Map<JMetaVarType, JType> instantiations() {
            return instantiations;
        }

        public Map<JVarType, JMetaVarType> metaVars() {
            return metaVars;
        }

        public JVarTypeResolveVisitor varTypeResolveVisitor() {
            return this.varTypeResolveVisitor;
        }

        public JMetaVarTypeResolveVisitor metaVarTypeResolveVisitor() {
            return this.metaVarTypeResolveVisitor;
        }

        @Override
        public JType apply(JType type) {
            JType result = type;

            if (!this.metaVars.isEmpty()) {
                result = this.varTypeResolveVisitor.visit(result);
            }

            if (!this.instantiations.isEmpty()) {
                result = this.metaVarTypeResolveVisitor.visit(result);
            }

            return result;
        }

        public Collection<JType> apply(Collection<JType> types, Supplier<Collection<JType>> constructor) {
            return types.stream().map(this).collect(Collectors.toCollection(constructor));
        }

        @Override
        public String toString() {
            return "TypeMetavarMap[" +
                    "instantiations=" + instantiations + ", " +
                    "metaVars=" + metaVars + ']';
        }


        @Override
        public String simpleName() {
            String mvs = metaVars.entrySet().stream().map(e -> e.getKey().simpleName() + " -> " + e.getValue().simpleName()).collect(Collectors.joining(", "));
            String insts = instantiations.entrySet().stream().map(e -> e.getKey().simpleName() + " = " + e.getValue().simpleName()).collect(Collectors.joining(", "));

            if (!metaVars.isEmpty() && !instantiations.isEmpty()) {
                return "{" + mvs + ", " + insts + "}";
            } else if (!metaVars.isEmpty()) {
                return "{" + mvs + "}";
            } else if (!instantiations.isEmpty()) {
                return "{" + insts + "}";
            } else {
                return "{}";
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            TypeMetavarMap that = (TypeMetavarMap) object;
            return Objects.equals(instantiations, that.instantiations) && Objects.equals(metaVars, that.metaVars);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instantiations, metaVars);
        }
    }
}
