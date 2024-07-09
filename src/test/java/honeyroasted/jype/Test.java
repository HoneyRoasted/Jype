package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver._old.solvers.inference.helper.TypeCompatibilityChecker;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver._old.solvers.inference.helper.TypeBoundResolver;
import honeyroasted.jype.system.solver._old.solvers.inference.helper.TypeConstraintReducer;
import honeyroasted.jype.system.solver.solvers.CompatibilityTypeSolver_2;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;

import java.util.List;
import java.util.Set;

public class Test {

    public static <T extends List<? extends T>> void main(String[] args) {

    }

}
