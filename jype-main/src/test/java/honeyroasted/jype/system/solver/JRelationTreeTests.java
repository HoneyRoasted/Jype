package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JRelationTreeTests {
    public static ArrayList<String> arrayListOfString;
    public static List<? extends CharSequence> listOfCharSequence;
    public static List<List<String>> nestedList;
    public static List<? extends List<? extends CharSequence>> nestedWildcardList;

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;

    private static JType type(String field) throws NoSuchFieldException {
        return TS.tryResolve(JRelationTreeTests.class.getField(field).getGenericType());
    }

    private static ConstraintTree solve(Constraint constraint, JSolverConfig config) {
        return TS.operations().compatibilitySolver(config).bind(constraint).solve();
    }

    private static ConstraintBranch only(Set<ConstraintBranch> branches) {
        assertEquals(1, branches.size(), branches.toString());
        return branches.iterator().next();
    }

    private static Optional<Constraint> premiseOf(ConstraintBranch branch, Constraint parent, Class<? extends Constraint> kind) {
        return branch.constraints().keySet().stream()
                .filter(kind::isInstance)
                .filter(c -> branch.allMetadata(c, JTypeContext.PremiseOf.class).stream().anyMatch(p -> p.judgment().equals(parent)))
                .findFirst();
    }

    @Test
    public void proofIsBuiltAsPremiseConstraints() throws NoSuchFieldException {
        JTypeConstraints.Subtype root = new JTypeConstraints.Subtype(type("arrayListOfString"), type("listOfCharSequence"));
        ConstraintTree tree = solve(root, JSolverConfig.EXPLAINED);
        assertEquals(true, tree.status().isTrue());
        ConstraintBranch branch = only(tree.validBranches());
        assertEquals(Constraint.Status.TRUE, branch.status(root));
        assertTrue(branch.firstMetadata(root, JTypeContext.RelationRule.class).map(r -> r.rule().startsWith("JLS §4.10.2 supertype")).orElse(false), branch.toString());

        Constraint contains = premiseOf(branch, root, JTypeConstraints.Contains.class).orElseThrow();
        assertEquals(Constraint.Status.TRUE, branch.status(contains));
        assertEquals("JLS §4.5.1 T <= ? extends S if T <: S", branch.firstMetadata(contains, JTypeContext.RelationRule.class).orElseThrow().rule());

        Constraint stringSubtype = premiseOf(branch, contains, JTypeConstraints.Subtype.class).orElseThrow();
        assertTrue(((JTypeConstraints.Subtype) stringSubtype).right().typeEquals(TS.tryResolve(CharSequence.class)));
        assertEquals(Constraint.Status.TRUE, branch.status(stringSubtype));
    }

    @Test
    public void failedAttemptsBecomeInvalidSiblingBranches() {
        JType intersection = JIntersectionType.of(new LinkedHashSet<>(List.of(TS.tryResolve(Runnable.class), TS.tryResolve(CharSequence.class))), TS);
        JType runnable = TS.tryResolve(Runnable.class);
        JType charSequence = TS.tryResolve(CharSequence.class);
        JTypeConstraints.Subtype root = new JTypeConstraints.Subtype(intersection, charSequence);
        JTypeConstraints.Subtype attempt = new JTypeConstraints.Subtype(runnable, charSequence);

        ConstraintTree explained = solve(root, JSolverConfig.EXPLAINED);
        assertEquals(true, explained.status().isTrue());
        ConstraintBranch proof = only(explained.validBranches());
        assertEquals(false, proof.constraints().containsKey(attempt), proof.toString());
        assertTrue(explained.invalidBranches().stream().anyMatch(b -> b.status(attempt) == Constraint.Status.FALSE), explained.toString());

        ConstraintTree proofsOnly = solve(root, JSolverConfig.PROOFS);
        assertEquals(true, proofsOnly.status().isTrue());
        assertEquals(0, proofsOnly.invalidBranches().size(), proofsOnly.toString());
    }

    @Test
    public void failedJudgmentKeepsItsReason() {
        JTypeConstraints.Subtype root = new JTypeConstraints.Subtype(TS.tryResolve(Integer.class), TS.tryResolve(String.class));
        ConstraintTree tree = solve(root, JSolverConfig.EXPLAINED);
        assertEquals(false, tree.status().isTrue());
        ConstraintBranch branch = tree.branches().iterator().next();
        assertEquals(Constraint.Status.FALSE, branch.status(root));
        assertTrue(branch.firstMetadata(root, JTypeContext.RelationRule.class).orElseThrow().rule().contains("is not a supertype"));
    }

    @Test
    @Timeout(60)
    public void cyclicCheckTerminatesInTheTree() throws NoSuchFieldException {
        JType cyclic = TS.tryResolve(JTypeRelationsCycleTests.Cyclic.class);
        JType target = TS.tryResolve(JTypeRelationsCycleTests.class.getField("superCyclic").getGenericType());
        ConstraintTree tree = solve(new JTypeConstraints.Subtype(cyclic, target), JSolverConfig.EXPLAINED);
        assertEquals(false, tree.status().isTrue());
        assertTrue(tree.branches().stream().anyMatch(b -> b.constraints().keySet().stream()
                .anyMatch(c -> b.allMetadata(c, JTypeContext.RelationRule.class).stream().anyMatch(r -> r.rule().equals("cyclic subtype check")))), tree.toString());
    }

    @Test
    public void relationDepthComesFromTheConfig() throws NoSuchFieldException {
        JTypeConstraints.Subtype root = new JTypeConstraints.Subtype(type("nestedList"), type("nestedWildcardList"));
        assertEquals(true, solve(root, JSolverConfig.EXPLAINED).status().isTrue());
        assertEquals(false, solve(root, JSolverConfig.EXPLAINED.withMaxRelationDepth(1)).status().isTrue());
        assertEquals(false, solve(root, JSolverConfig.FAST.withMaxRelationDepth(1)).status().isTrue());
    }

    @Test
    public void fastConfigBuildsNoPremises() throws NoSuchFieldException {
        JTypeConstraints.Subtype root = new JTypeConstraints.Subtype(type("arrayListOfString"), type("listOfCharSequence"));
        ConstraintBranch branch = only(solve(root, JSolverConfig.FAST).validBranches());
        assertEquals(1, branch.constraints().size(), branch.toString());
    }

    @Test
    public void fastAndTreeSolversAgree() throws NoSuchFieldException {
        List<JType> types = List.of(type("arrayListOfString"), type("listOfCharSequence"), type("nestedList"), type("nestedWildcardList"),
                TS.tryResolve(Integer.class), TS.tryResolve(String.class), TS.constants().intType(), TS.constants().object());
        for (JType s : types) {
            for (JType t : types) {
                for (JTypeConstraints.Compatible.Context context : JTypeConstraints.Compatible.Context.values()) {
                    JTypeConstraints.Compatible constraint = new JTypeConstraints.Compatible(s, context, t);
                    assertEquals(JTypeRelations.isCompatible(s, context, t), solve(constraint, JSolverConfig.EXPLAINED).status().isTrue(), constraint.simpleName());
                }
                JTypeConstraints.Subtype subtype = new JTypeConstraints.Subtype(s, t);
                assertEquals(JTypeRelations.isSubtype(s, t), solve(subtype, JSolverConfig.EXPLAINED).status().isTrue(), subtype.simpleName());
            }
        }
    }

    @Test
    public void inferenceSolverRecordsProofsByDefault() throws NoSuchFieldException {
        JExpressionInformation expr = JExpressionInformation.of(type("arrayListOfString"));
        JTypeConstraints.ExpressionCompatible root = new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, type("listOfCharSequence"));
        ConstraintBranch proof = only(TS.operations().inferenceSolver().bind(root).solve().validBranches());
        assertTrue(proof.constraints().keySet().stream().anyMatch(c -> c instanceof JTypeConstraints.Contains && proof.hasMetadata(c, JTypeContext.PremiseOf.class)), proof.toString());
        ConstraintBranch fast = only(TS.operations().inferenceSolver(JSolverConfig.FAST).bind(root).solve().validBranches());
        assertEquals(false, fast.constraints().keySet().stream().anyMatch(c -> c instanceof JTypeConstraints.Contains), fast.toString());
    }
}
