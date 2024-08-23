package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BuildInitialBounds implements ConstraintMapper {

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
        Map<VarType, MetaVarType> metaVars = new LinkedHashMap<>();
        Map<VarType, ConstraintNode> source = new HashMap<>();

        bounds.neighbors(ConstraintNode.Operation.AND).forEach(cn -> {
            if (cn.constraint() instanceof TypeConstraints.Infer inf) {
                metaVars.put(inf.right(), inf.left());
                source.put(inf.right(), cn);
            }
        });

        VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);
        metaVars.forEach((vt, mvt) -> {
            TrackedConstraint parent = source.get(vt).trackedConstraint();
            if (vt.upperBounds().isEmpty()) {
                //Case where P_l has no upper bound, alpha_l <: Object
                bounds.attach(new TypeConstraints.Subtype(mvt, vt.typeSystem().constants().object())
                        .tracked(parent).createLeaf().overrideStatus(true));
            } else {
                //Case where P_l has upper bounds
                boolean foundProperUpper = false;
                for (Type bound : vt.upperBounds()) {
                    Type resolved = resolver.visit(bound);
                    if (resolved.isProperType()) {
                        foundProperUpper = true;
                    }
                    //Upper bounds imply alpha_l <: T[P_1 = alpha_1... P_n = alpha_n], for each bound T
                    bounds.attach(new TypeConstraints.Subtype(mvt, resolved)
                            .tracked(parent).createLeaf().overrideStatus(true));
                }

                if (!foundProperUpper) {
                    //If there is no proper upper bound, alpha_l <: Object
                    bounds.attach(new TypeConstraints.Subtype(mvt, vt.typeSystem().constants().object())
                            .tracked(parent).createLeaf().overrideStatus(true));
                }
            }
        });
    }

}
