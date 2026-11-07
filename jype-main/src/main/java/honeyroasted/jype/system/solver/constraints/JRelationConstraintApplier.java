package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.applier.ConstraintMapperApplier;
import honeyroasted.jype.system.solver.JSolverConfig;
import honeyroasted.jype.system.solver.JTypeRelations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JRelationConstraintApplier implements ConstraintMapperApplier {
    private final List<ConstraintMapper> mappers;
    private final JSolverConfig config;

    public JRelationConstraintApplier(List<ConstraintMapper> mappers, JSolverConfig config) {
        this.mappers = Collections.unmodifiableList(mappers);
        this.config = config;
    }

    public JSolverConfig config() {
        return this.config;
    }

    @Override
    public List<ConstraintMapper> mappers() {
        return this.mappers;
    }

    @Override
    public List<ConstraintMapper> flattened() {
        List<ConstraintMapper> flat = new ArrayList<>();
        for (ConstraintMapper mapper : this.mappers) {
            if (mapper instanceof ConstraintMapperApplier applier) {
                flat.addAll(applier.flattened());
            } else {
                flat.add(mapper);
            }
        }
        return flat;
    }

    @Override
    public void accept(ConstraintTree tree) {
        do {
            for (ConstraintBranch branch : new ArrayList<>(tree.active())) {
                this.accept(branch);
            }
        } while (tree.executeChanges());
    }

    @Override
    public void accept(ConstraintBranch branch) {
        List<Constraint> asks = new ArrayList<>();
        for (Map.Entry<Constraint, Constraint.Status> entry : branch.constraints().entrySet()) {
            if (entry.getValue().isUnknown() && JTypeRelations.isRelationConstraint(entry.getKey())) {
                asks.add(entry.getKey());
            }
        }
        for (Constraint ask : asks) {
            JTypeRelations.ask(branch, ask, this.config);
        }
        for (ConstraintMapper mapper : this.mappers) {
            mapper.accept(branch);
        }
    }
}
