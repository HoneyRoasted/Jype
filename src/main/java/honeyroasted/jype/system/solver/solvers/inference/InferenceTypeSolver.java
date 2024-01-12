package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AbstractTypeSolver;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeBoundResolver;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeConstraintReducer;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.Map;
import java.util.Set;

public class InferenceTypeSolver extends AbstractTypeSolver {
    private TypeConstraintReducer constraintReducer;
    private TypeBoundResolver resolver;

    public InferenceTypeSolver() {
        super(Set.of(TypeBound.ExpressionCompatible.class,
                TypeBound.Compatible.class,
                TypeBound.Subtype.class,
                TypeBound.Contains.class,
                TypeBound.Equal.class,
                TypeBound.LambdaThrows.class));

        this.constraintReducer = new TypeConstraintReducer(this);
        this.resolver = new TypeBoundResolver(this);
    }

    @Override
    public void reset() {
        this.constraintReducer.reset();
    }

    @Override
    public Result solve(TypeSystem system) {
        return null;
    }

    public static void discoverVarTypes(Type visit, Map<VarType, MetaVarType> metaVars, Set<VarType> ignore) {
        new RecursiveTypeVisitor<>((TypeVisitor.Default) (type, context) -> {
            if (type instanceof VarType vt && !ignore.contains(vt) && !metaVars.containsKey(vt)) {
                metaVars.put(vt, new MetaVarTypeImpl(vt.typeSystem(), System.identityHashCode(vt), vt.name()));
            }
            return null;
        }, null, false).visit(visit);
    }

}
