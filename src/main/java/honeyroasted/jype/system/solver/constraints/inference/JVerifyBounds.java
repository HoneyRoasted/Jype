package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeContext;

public class JVerifyBounds implements ConstraintMapper {
    @Override
    public void accept(ConstraintBranch branch) {
        if (branch.status().isTrue()) {
            JTypeSystem system = branch.parent().metadata().firstOr(JTypeSystem.class, JTypeSystem.RUNTIME_REFLECTION);
            JTypeContext.JTypeMapper mapper = new JTypeContext.JTypeMapper(brnch -> system.operations().varTypeMapper().mapper().apply(brnch)
                    .andThen(system.operations().metaVarTypeMapper().mapper().apply(brnch)));

            PropertySet context = new PropertySet()
                    .attach(system)
                    .attach(mapper);

            branch.constraints().forEach((con, status) -> {
                if (status == Constraint.Status.ASSUMED) {
                    Constraint.Status check = system.operations().checkStatus(con, context);
                    if (check.isKnown()) {
                        branch.set(con, check);
                    } else {
                        branch.set(con, Constraint.Status.TRUE);
                    }
                }
            });
        }
    }
}
