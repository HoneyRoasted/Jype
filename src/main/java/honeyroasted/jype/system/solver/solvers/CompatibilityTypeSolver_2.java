package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleExplicitCast;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleLooseInvocation;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleSubtype;
import honeyroasted.jype.system.solver.solvers.compatibility.EqualType;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionAssignmentConstant;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionSimplyTyped;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeArray;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeEquality;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeGenericClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeIntersection;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeMetaVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeNone;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypePrimitive;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeRawClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeUnchecked;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeWild;

import java.util.List;
import java.util.Set;

public class CompatibilityTypeSolver_2 extends TypeBoundMapperSolver {

    public CompatibilityTypeSolver_2() {
        super("CompatibilityTypeSolver",
                Set.of(TypeBound.Equal.class, TypeBound.Subtype.class, TypeBound.Compatible.class),
                new TypeBoundMapperApplier(List.of(
                        new EqualType(),
                        new CompatibleExplicitCast(),
                        new CompatibleLooseInvocation(),
                        new CompatibleSubtype(),
                        new ExpressionAssignmentConstant(),
                        new ExpressionSimplyTyped(),

                        new SubtypeNone(),
                        new SubtypePrimitive(),
                        new SubtypeEquality(),
                        new SubtypeUnchecked(),
                        new SubtypeRawClass(),
                        new SubtypeGenericClass(),
                        new SubtypeArray(),
                        new SubtypeVar(),
                        new SubtypeMetaVar(),
                        new SubtypeWild(),
                        new SubtypeIntersection()
                )));
    }

}
