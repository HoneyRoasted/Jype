package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AbstractTypeSolver;
import honeyroasted.jype.system.solver.solvers.inference.expression.ExpressionResolver;
import honeyroasted.jype.system.solver.solvers.inference.helper.InitialBoundBuilder;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeIncorporater;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class InferenceTypeSolver extends AbstractTypeSolver {
    private ExpressionResolver expressionResolver;
    private TypeIncorporater incorporater;
    private InitialBoundBuilder initialBoundBuilder;

    public InferenceTypeSolver(ExpressionResolver expressionResolver) {
        super(Set.of(TypeBound.Equal.class,
                TypeBound.Compatible.class, TypeBound.ExpressionCompatible.class,
                TypeBound.Contains.class, TypeBound.LambdaThrows.class,
                TypeBound.Throws.class, TypeBound.Capture.class,
                TypeBound.Subtype.class));
        this.expressionResolver = expressionResolver;

        this.incorporater = new TypeIncorporater(this);
        this.initialBoundBuilder = new InitialBoundBuilder(this);
    }

    private Set<TypeBound.Result.Builder> workingBounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> workingConstrains = new LinkedHashSet<>();

    @Override
    public void reset() {
        this.workingBounds.clear();
        this.workingConstrains.clear();

        this.incorporater.reset();
        this.initialBoundBuilder.reset();
    }

    @Override
    public Result solve(TypeSystem system) {
        return null;
    }


    public static void discoverVarTypes(Type visit, Map<VarType, MetaVarType> metaVars) {
        new RecursiveTypeVisitor<>((TypeVisitor.Default) (type, context) -> {
            if (type instanceof VarType vt && !metaVars.containsKey(vt)) {
                metaVars.put(vt, new MetaVarTypeImpl(vt.typeSystem(), System.identityHashCode(vt), vt.name()));
            }
            return null;
        }, null, false).visit(visit);
    }

}
