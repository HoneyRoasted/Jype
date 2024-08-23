package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashMap;
import java.util.Map;

public class MetaVarTypeMapper implements ConstraintMapper {
    @Override
    public int arity() {
        return PARENT_BRANCH_NODE;
    }

    @Override
    public boolean filter(PropertySet context, ConstraintNode node) {
        return true;
    }

    @Override
    public boolean accepts(PropertySet context, ConstraintNode... nodes) {
        return true;
    }

    @Override
    public void process(PropertySet context, ConstraintNode... nodes) {
        ConstraintTree bounds = nodes[0].expandRoot(ConstraintNode.Operation.AND);
        Map<MetaVarType, Type> instantiations = new LinkedHashMap<>();

        bounds.neighbors(ConstraintNode.Operation.AND).forEach(cn -> {
            if (cn.constraint() instanceof TypeConstraints.Instantiation inst) {
                instantiations.put(inst.left(), inst.right());
            }
        });

        if (instantiations.isEmpty()) {
            context.attach(TypeConstraints.NO_OP);
        } else {
            MetaVarTypeResolver resolver = new MetaVarTypeResolver(instantiations);
            context.attach(new TypeConstraints.TypeMapper(resolver::visit));
        }
    }
}
