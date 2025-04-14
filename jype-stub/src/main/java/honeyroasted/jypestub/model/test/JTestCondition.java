package honeyroasted.jypestub.model.test;

import com.fasterxml.jackson.core.type.TypeReference;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jypestub.model.JStubSerialization;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface JTestCondition {
    Map<String, Class<? extends JTestCondition>> SUBTYPE_KEYS = Map.of(
            "status", Status.class,
            "instantiation", Instantiation.class
    );

    Result test(JTypeSystem system, ConstraintTree tree);

    record Result(String message, boolean result) {
        public static Result success() {
            return success("Condition passed");
        }

        public static Result success(String message) {
            return new Result(message, true);
        }

        public static Result fail() {
            return fail("Condition failed");
        }

        public static Result fail(String message) {
            return new Result(message, false);
        }
    }

    record Wrapper(JTestCondition inner) implements JStubSerialization.Wrapper<JTestCondition> {

        public Result test(JTypeSystem system, ConstraintTree tree) {
            return this.inner.test(system, tree);
        }

        public static JStubSerialization.WrapperSerializer<JTestCondition, Wrapper> SERIALIZER = new JStubSerialization.WrapperSerializer<>(Wrapper.class, SUBTYPE_KEYS);
        public static JStubSerialization.WrapperDeserializer<JTestCondition, Wrapper> DESERIALIZER = new JStubSerialization.WrapperDeserializer<>(Wrapper.class, SUBTYPE_KEYS, Wrapper::new);
    }

    record Status(boolean status) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            return new Result("Expected status " + this.status() + ", got " + tree.status().isTrue(), tree.status().isTrue() == this.status());
        }

        public static JStubSerialization.UnwrappingSerializer<Status, Boolean> SERIALIZER = new JStubSerialization.UnwrappingSerializer<>(Status.class, Status::status);
        public static JStubSerialization.WrappingDeserializer<Status, Boolean> DESERIALIZER = new JStubSerialization.WrappingDeserializer<>(Status.class, new TypeReference<Boolean>() {}, Status::new);
    }

    record Instantiation(String var, String declaring, String declaringMethod, String type) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            JGenericDeclaration declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            JType target = JStubSerialization.readType(system, type, declaring);

            for (ConstraintBranch cb : tree.validBranches()) {
                Optional<JTypeContext.TypeMetavarMap> opt = cb.metadata().first(JTypeContext.TypeMetavarMap.class);
                if (opt.isPresent() && opt.get().instantiations().entrySet().stream().anyMatch(e -> e.getKey().name().equals(this.var) && e.getValue().typeEquals(target))) {
                    return new Result("Variable is equal to " + target + " (via metadata)", true);
                }

                if (findInstantiations(this.var, cb).stream().anyMatch(found -> found.typeEquals(target))) {
                    return new Result("Variable is equal to " + target + " (via constraints)", true);
                }
            }
            return new Result("Could not find variable with name " + this.var() + " equal to " + target, false);
        }

        private static Set<JType> findInstantiations(String mvt, ConstraintBranch bounds) {
            Set<JType> types = new LinkedHashSet();
            bounds.constraints().forEach((bound, status) -> {
                if (status.isTrue()) {
                    if (bound instanceof JTypeConstraints.Equal eq) {
                        if (eq.left() instanceof JMetaVarType emvt && emvt.name().equals(mvt) && eq.right().isProperType()) {
                            types.add(eq.right());
                        } else if (eq.right() instanceof JMetaVarType emvt && emvt.name().equals(mvt) && eq.left().isProperType()) {
                            types.add(eq.left());
                        }
                    } else if (bound instanceof JTypeConstraints.Instantiation inst) {
                        if (inst.left() instanceof JMetaVarType emvt && emvt.name().equals(mvt)) {
                            types.add(inst.right());
                        }
                    }
                }

            });

            return types;
        }
    }
}
