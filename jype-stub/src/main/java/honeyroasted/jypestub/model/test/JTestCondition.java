package honeyroasted.jypestub.model.test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
            "instantiation", Instantiation.class,
            "constraint", Constraint.class,
            "infer_cons", InferConstraint.class,
            "instantiation_cons", InstantiationConstraint.class,
            "throws_cons", ThrowsConstraint.class
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

    enum Branch {
        ALL, VALID, INVALID
    }

    enum Op {
        AND, OR
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

    record Instantiation(String var, String declaring, String declaringMethod, String type, Branch branches) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            JGenericDeclaration declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            JType target = JStubSerialization.readType(system, type, declaring);

            for (ConstraintBranch cb : JStubSerialization.branches(tree, this.branches)) {
                Optional<JTypeContext.TypeMetavarMap> opt = cb.metadata().first(JTypeContext.TypeMetavarMap.class);
                if (opt.isPresent() && opt.get().instantiations().entrySet().stream().anyMatch(e -> e.getKey().name().equals(this.var) && e.getValue().typeEquals(target))) {
                    return new Result(this.var + " is equal to " + target + " (via metadata)", true);
                }

                if (findInstantiations(this.var, cb).stream().anyMatch(found -> found.typeEquals(target))) {
                    return new Result(this.var + " is equal to " + target + " (via constraints)", true);
                }
            }
            return new Result("Could not find variable with name " + this.var + " equal to " + target, false);
        }

        private static Set<JType> findInstantiations(String mvt, ConstraintBranch bounds) {
            Set<JType> types = new LinkedHashSet<>();
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

    record Constraint(@JsonUnwrapped JTestConstraint.Wrapper constraint, Boolean status, Op operation, Branch branches) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            boolean targetStatus = this.status == null ? true : this.status;
            Op op = this.operation == null ? Op.AND : Op.OR;

            Set<honeyroasted.almonds.Constraint> cons = new LinkedHashSet<>(this.constraint.resolve(system));
            int initial = cons.size();

            for (ConstraintBranch branch : JStubSerialization.branches(tree, this.branches)) {
                for (honeyroasted.almonds.Constraint con : cons) {
                    honeyroasted.almonds.Constraint.Status status = branch.constraints().get(con);
                    if (status != null && status.isTrue() == targetStatus) {
                        cons.remove(con);
                    }
                }
            }

            if (op == Op.OR && cons.size() == initial) {
                return new Result("Found at least one target constraint in a valid branch", true);
            } else if (op == Op.AND && cons.isEmpty()) {
                return new Result("Found all target constraints in a valid branch", true);
            } else {
                return new Result("Missing required constraints: " + cons + " with status " + targetStatus, false);
            }
        }
    }

    record InferConstraint(String metaVar, String var, Boolean status, Branch branches) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            boolean targetStatus = this.status == null ? true : this.status;

            for (ConstraintBranch branch : JStubSerialization.branches(tree, this.branches)) {
                for (Map.Entry<honeyroasted.almonds.Constraint, honeyroasted.almonds.Constraint.Status> cons : branch.constraints().entrySet()) {
                    if (cons.getKey() instanceof JTypeConstraints.Infer infer) {
                        if (infer.left().name().equals(this.metaVar) && infer.right().name().equals(this.var)) {
                            if (cons.getValue().isTrue() == targetStatus) {
                                return new Result("Found required constraint in a valid branch", true);
                            }
                        }
                    }
                }
            }

            return new Result("Missing required constraint: " + "infer(" + this.metaVar + " = " + this.var + ") with status " + targetStatus, false);
        }
    }

    record InstantiationConstraint(String metaVar, String declaring, String declaringMethod, String type, Boolean status, Branch branches) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            JGenericDeclaration declaring = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            JType target = JStubSerialization.readType(system, type, declaring);
            boolean targetStatus = this.status == null ? true : this.status;

            for (ConstraintBranch branch : JStubSerialization.branches(tree, this.branches)) {
                for (Map.Entry<honeyroasted.almonds.Constraint, honeyroasted.almonds.Constraint.Status> cons : branch.constraints().entrySet()) {
                    if (cons.getKey() instanceof JTypeConstraints.Instantiation inst) {
                        if (inst.left().name().equals(this.metaVar) && inst.right().typeEquals(target)) {
                            if (cons.getValue().isTrue() == targetStatus) {
                                return new Result("Found required constraint in a valid branch", true);
                            }
                        }
                    }
                }
            }

            return new Result("Missing required constraint: " + "instantiation(" + this.metaVar + " = " + targetStatus + ") with status " + targetStatus, false);
        }
    }

    record ThrowsConstraint(String metaVar, Boolean status, Branch branches) implements JTestCondition {
        @Override
        public Result test(JTypeSystem system, ConstraintTree tree) {
            boolean targetStatus = this.status == null ? true : this.status;

            for (ConstraintBranch branch : JStubSerialization.branches(tree, this.branches)) {
                for (Map.Entry<honeyroasted.almonds.Constraint, honeyroasted.almonds.Constraint.Status> cons : branch.constraints().entrySet()) {
                    if (cons.getKey() instanceof JTypeConstraints.Throws thr) {
                        if (thr.value().name().equals(this.metaVar)) {
                            if (cons.getValue().isTrue() == targetStatus) {
                                return new Result("Found required constraint in a valid branch", true);
                            }
                        }
                    }
                }
            }

            return new Result("Missing required constraint: " + "throws(" + this.metaVar + ") with status " + targetStatus, false);
        }
    }
}
