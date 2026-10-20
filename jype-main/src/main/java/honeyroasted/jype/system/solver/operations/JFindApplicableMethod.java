package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JFindApplicableMethod implements JTypeOperation<JExpressionInformation.MethodInvocation<?>, JParameterizedMethodType> {

    private enum Phase {
        STRICT, LOOSE, VARIABLE_ARITY
    }

    private record TypeParamResolution(List<JTypeConstraints.Infer> inferConstraints, Map<JVarType, JMetaVarType> metaVars,
                                        JVarTypeResolveVisitor resolver) {
    }

    private record Candidate(JMethodReference reference, TypeParamResolution typeParams, ConstraintBranch branch,
                              List<JType> effectiveParameters) {
    }

    @Override
    public JParameterizedMethodType apply(JTypeSystem system, JExpressionInformation.MethodInvocation<?> invocation) {
        JClassReference declaring = invocation.declaring();
        List<JExpressionInformation> arguments = invocation.parameters();

        Collection<JMethodReference> candidates;
        boolean staticOnly;
        JType receiverType = null;

        if (invocation.source() instanceof JClassReference source) {
            candidates = JMemberLookup.staticMethods(source);
            staticOnly = true;
        } else if (invocation.source() instanceof JExpressionInformation expr) {
            staticOnly = false;

            if (expr.isSimplyTyped()) {
                receiverType = expr.getSimpleType(system, Function.identity()).orElse(null);
            } else if (expr instanceof JExpressionInformation.MethodInvocation<?> chained) {
                JParameterizedMethodType chainedResult = this.apply(system, chained);
                if (chainedResult != null) {
                    receiverType = chainedResult.varTypeResolver().apply(chainedResult.returnType());
                }
            }

            if (receiverType == null) {
                return null;
            }

            candidates = methodsOn(receiverType);
        } else {
            return null;
        }

        JClassType receiverClassType = receiverType instanceof JClassType rct ? rct : null;
        List<JMethodReference> named = candidates.stream()
                .filter(ref -> ref.location().name().equals(invocation.name()))
                .filter(ref -> ref.outerClass().accessFrom(declaring, receiverClassType).canAccess(ref.access()))
                .filter(ref -> !staticOnly || ref.hasModifier(AccessFlag.STATIC))
                .filter(ref -> invocation.explicitTypeArguments().isEmpty() || invocation.explicitTypeArguments().size() == ref.typeParameters().size())
                .toList();

        if (named.isEmpty()) {
            return null;
        }

        for (Phase phase : Phase.values()) {
            List<Candidate> applicable = new ArrayList<>();

            for (JMethodReference ref : named) {
                Candidate candidate = tryApply(system, ref, invocation, arguments, phase);
                if (candidate != null) {
                    applicable.add(candidate);
                }
            }

            if (!applicable.isEmpty()) {
                Candidate chosen = mostSpecific(system, applicable);
                return chosen == null ? null : instantiate(chosen);
            }
        }

        return null;
    }

    private static Candidate tryApply(JTypeSystem system, JMethodReference ref, JExpressionInformation.MethodInvocation<?> invocation,
                                       List<JExpressionInformation> arguments, Phase phase) {
        int paramCount = ref.parameters().size();
        boolean vararg = phase == Phase.VARIABLE_ARITY;

        if (vararg) {
            if (!ref.hasModifier(AccessFlag.VARARGS) || paramCount == 0 || arguments.size() < paramCount - 1 ||
                    !(ref.parameters().get(paramCount - 1) instanceof JArrayType)) {
                return null;
            }
        } else if (paramCount != arguments.size()) {
            return null;
        }

        JTypeConstraints.Compatible.Context context = phase == Phase.STRICT ?
                JTypeConstraints.Compatible.Context.STRICT_INVOCATION : JTypeConstraints.Compatible.Context.LOOSE_INVOCATION;

        TypeParamResolution typeParams = resolveTypeParams(ref, invocation);

        List<Constraint> constraints = new ArrayList<>();
        constraints.add(Constraint.TRUE);
        constraints.addAll(typeParams.inferConstraints());
        List<JType> effective = new ArrayList<>();

        int fixed = vararg ? paramCount - 1 : paramCount;
        for (int i = 0; i < fixed; i++) {
            JType paramType = typeParams.resolver().apply(ref.parameters().get(i));
            effective.add(paramType);
            constraints.add(new JTypeConstraints.ExpressionCompatible(arguments.get(i), context, paramType));
        }

        if (vararg) {
            JArrayType varargType = (JArrayType) typeParams.resolver().apply(ref.parameters().get(paramCount - 1));
            JType component = varargType.component();
            for (int i = fixed; i < arguments.size(); i++) {
                effective.add(component);
                constraints.add(new JTypeConstraints.ExpressionCompatible(arguments.get(i), context, component));
            }
        }

        ConstraintTree tree = system.operations().inferenceSolver().bind(constraints.toArray(new Constraint[0])).solve();
        ConstraintBranch branch = tree.validBranches().stream().findFirst().orElse(null);

        return branch == null ? null : new Candidate(ref, typeParams, branch, effective);
    }

    private static TypeParamResolution resolveTypeParams(JMethodReference ref, JExpressionInformation.MethodInvocation<?> invocation) {
        if (ref.typeParameters().isEmpty()) {
            return new TypeParamResolution(List.of(), Map.of(), new JVarTypeResolveVisitor(Map.of()));
        }

        Map<JVarType, JType> resolution = new LinkedHashMap<>();
        Map<JVarType, JMetaVarType> metaVars = new LinkedHashMap<>();
        List<JTypeConstraints.Infer> infer = new ArrayList<>();

        if (invocation.explicitTypeArguments().isEmpty()) {
            for (JVarType typeParam : ref.typeParameters()) {
                JMetaVarType metaVar = typeParam.createMetaVar();
                infer.add(new JTypeConstraints.Infer(metaVar, typeParam));
                metaVars.put(typeParam, metaVar);
                resolution.put(typeParam, metaVar);
            }
        } else {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                resolution.put(ref.typeParameters().get(i), invocation.explicitTypeArguments().get(i));
            }
        }

        return new TypeParamResolution(infer, metaVars, new JVarTypeResolveVisitor(resolution));
    }

    private static JParameterizedMethodType instantiate(Candidate candidate) {
        JMethodReference ref = candidate.reference();
        if (ref.typeParameters().isEmpty()) {
            return ref.parameterizedWithTypeVars();
        }

        List<JArgumentType> typeArguments = new ArrayList<>();
        for (JVarType typeParam : ref.typeParameters()) {
            JMetaVarType metaVar = candidate.typeParams().metaVars().get(typeParam);
            if (metaVar == null) {
                typeArguments.add((JArgumentType) candidate.typeParams().resolver().apply(typeParam));
            } else {
                JType instantiation = JResolveBounds.findInstantiation(metaVar, candidate.branch());
                if (instantiation == null) {
                    return null;
                }
                typeArguments.add((JArgumentType) instantiation);
            }
        }

        return ref.parameterized(typeArguments);
    }

    private static Candidate mostSpecific(JTypeSystem system, List<Candidate> candidates) {
        List<Candidate> maximal = new ArrayList<>();

        for (Candidate candidate : candidates) {
            boolean isMaximal = true;
            for (Candidate other : candidates) {
                if (candidate != other && !isMoreSpecific(system, candidate, other)) {
                    isMaximal = false;
                    break;
                }
            }
            if (isMaximal) {
                maximal.add(candidate);
            }
        }

        if (maximal.isEmpty()) {
            return null;
        } else if (maximal.size() == 1) {
            return maximal.getFirst();
        }

        List<Candidate> nonAbstract = maximal.stream().filter(c -> !Modifier.isAbstract(c.reference().modifiers())).toList();
        if (nonAbstract.size() == 1) {
            return nonAbstract.getFirst();
        } else if (nonAbstract.isEmpty()) {
            return maximal.getFirst();
        } else {
            return null;
        }
    }

    private static boolean isMoreSpecific(JTypeSystem system, Candidate left, Candidate right) {
        int size = left.effectiveParameters().size();
        for (int i = 0; i < size; i++) {
            if (!system.operations().isSubtype(left.effectiveParameters().get(i), right.effectiveParameters().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static Collection<JMethodReference> methodsOn(JType type) {
        Collection<JMethodReference> methods = new LinkedHashSet<>();
        JMemberLookup.findClassTypes(type).forEach(ct -> JMemberLookup.allMethods(ct.classReference()).forEach(ref -> {
            if (ct instanceof JParameterizedClassType pct) {
                ref = (JMethodReference) pct.varTypeResolver().apply(ref);
            }
            methods.add(ref);
        }));
        return methods;
    }

}
