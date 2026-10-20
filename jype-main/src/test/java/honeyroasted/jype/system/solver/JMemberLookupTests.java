package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JMemberLookupTests {

    @Test
    public void findClassTypesReturnsAllMembersOfAnIntersectionType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference number = ts.tryResolve(Number.class);
        JClassReference comparable = ts.tryResolve(Comparable.class);
        JType intersection = JIntersectionType.of(Set.of(number, comparable), ts);

        Set<JClassType> found = JMemberLookup.findClassTypes(intersection);

        assertTrue(found.stream().anyMatch(ct -> ct.classReference().typeEquals(number)),
                "expected the Number member of the intersection to be found");
        assertTrue(found.stream().anyMatch(ct -> ct.classReference().typeEquals(comparable)),
                "expected the Comparable member of the intersection to be found");
    }

    /**
     * Regression test: findClassTypes used to stop its BFS as soon as any branch of the
     * frontier resolved to a class type, silently dropping sibling branches that still needed
     * further hops. An inference variable can legitimately accumulate multiple upper bounds
     * where one (Comparable) resolves immediately and another (an unresolved metavar bounded
     * by Number) only resolves a hop later.
     */
    @Test
    public void findClassTypesDoesNotStopAtTheFirstResolvedBranchOfAMultiBoundMetaVar() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference number = ts.tryResolve(Number.class);
        JClassReference comparable = ts.tryResolve(Comparable.class);

        JMetaVarType inner = ts.typeFactory().newMetaVarType("T");
        inner.upperBounds().add(number);

        JMetaVarType outer = ts.typeFactory().newMetaVarType("U");
        outer.upperBounds().add(inner);
        outer.upperBounds().add(comparable);

        Set<JClassType> found = JMemberLookup.findClassTypes(outer);

        assertTrue(found.stream().anyMatch(ct -> ct.classReference().typeEquals(number)),
                "expected the indirect bound (via the inner metavar) to still be found, not just the direct Comparable bound");
        assertTrue(found.stream().anyMatch(ct -> ct.classReference().typeEquals(comparable)),
                "expected the direct Comparable bound to be found");
    }
}
