package honeyroasted.jype.system.solver;

import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.List;

public record JInvocationCandidate(JMethodReference method, List<JType> formals, List<JVarType> typeParameters, List<JVarType> explicitlyTypedParameters, JType result, List<JType> thrown, boolean variableArity) {
}
