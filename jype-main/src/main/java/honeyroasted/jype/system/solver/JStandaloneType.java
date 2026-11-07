package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.type.JType;

public record JStandaloneType(ConstraintBranch branch, Constraint extra, JType type) {
}
