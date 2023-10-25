package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AbstractTypeSolver;
import honeyroasted.jype.system.solver.solvers.inference.expression.ExpressionResolver;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeBoundIncorporater;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeCompatibilityChecker;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeInitialBoundBuilder;
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
    private TypeBoundIncorporater incorporater;
    private TypeInitialBoundBuilder initialBoundBuilder;
    private TypeCompatibilityChecker compatibilityChecker;

    public InferenceTypeSolver(ExpressionResolver expressionResolver) {
        super(Set.of(TypeBound.ExpressionCompatible.class,
                TypeBound.Compatible.class,
                TypeBound.Subtype.class,
                TypeBound.Contains.class,
                TypeBound.Equal.class,
                TypeBound.LambdaThrows.class));
        this.expressionResolver = expressionResolver;

        this.incorporater = new TypeBoundIncorporater(this);
        this.initialBoundBuilder = new TypeInitialBoundBuilder(this);
        this.compatibilityChecker = new TypeCompatibilityChecker(this);
    }

    private Set<TypeBound.Result.Builder> workingBounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> workingConstrains = new LinkedHashSet<>();
    private Set<MetaVarType> metaVars = new LinkedHashSet<>();

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

    private void updateMetavarBounds() {
        this.workingBounds.forEach(builder -> {
            TypeBound bound = builder.bound();
            if (bound instanceof TypeBound.Subtype st) {
                if (st.left() instanceof MetaVarType || st.right() instanceof MetaVarType) {
                    this.metaVars.forEach(mv -> {
                        if (st.left().typeEquals(mv)) {
                            mv.upperBounds().add(st.right());
                        }

                        if (st.right().typeEquals(mv)) {
                            mv.lowerBounds().add(st.right());
                        }
                    });
                }
            } else if (bound instanceof TypeBound.Equal eq) {
                if (eq.left() instanceof MetaVarType || eq.right() instanceof MetaVarType) {
                    this.metaVars.forEach(mv -> {
                        if (eq.left().typeEquals(mv)) {
                            mv.equalities().add(eq.right());
                        } else if (eq.right().typeEquals(mv)) {
                            mv.equalities().add(eq.left());
                        }
                        mv.equalities().remove(mv);
                    });
                }
            }
        });
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
