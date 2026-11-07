package honeyroasted.jype.system.solver;

import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.fixtures.protectedaccess.pkga.ProtectedBase;
import honeyroasted.jype.system.solver.fixtures.protectedaccess.pkgb.ProtectedSub1;
import honeyroasted.jype.system.solver.fixtures.protectedaccess.pkgb.ProtectedSub2;
import honeyroasted.jype.type.JClassReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the JLS §6.6.2 refinement for protected access via an explicit qualifying expression:
 * being in a subclass S of the declaring class C is not enough - the qualifying expression's
 * static type must also be S or a subclass of S. ProtectedBase (pkga) and ProtectedSub1/2 (pkgb)
 * live in different packages so the package-level fallback can't mask the protected check.
 */
public class JClassTypeAccessFromTests {

    @Test
    public void qualifyingExpressionTypedAsTheDeclaringClassCannotReachProtectedMember() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference base = ts.tryResolve(ProtectedBase.class);
        JClassReference sub1 = ts.tryResolve(ProtectedSub1.class);

        // Access occurs in ProtectedSub1 (a subclass of ProtectedBase), but the qualifying
        // expression's static type is ProtectedBase itself, not ProtectedSub1 or a subclass of it.
        JAccess result = base.accessFrom(sub1, base);

        assertFalse(result.canAccess(JAccess.PROTECTED),
                "a receiver typed as the declaring superclass must not reach a protected member from an unrelated subclass context");
    }

    @Test
    public void qualifyingExpressionTypedAsTheAccessingClassCanReachProtectedMember() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference base = ts.tryResolve(ProtectedBase.class);
        JClassReference sub1 = ts.tryResolve(ProtectedSub1.class);

        JAccess result = base.accessFrom(sub1, sub1);

        assertTrue(result.canAccess(JAccess.PROTECTED),
                "a receiver typed exactly as the accessing subclass should reach the protected member");
    }

    @Test
    public void qualifyingExpressionTypedAsAFurtherSubclassCanReachProtectedMember() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference base = ts.tryResolve(ProtectedBase.class);
        JClassReference sub1 = ts.tryResolve(ProtectedSub1.class);
        JClassReference sub2 = ts.tryResolve(ProtectedSub2.class);

        JAccess result = base.accessFrom(sub1, sub2);

        assertTrue(result.canAccess(JAccess.PROTECTED),
                "a receiver typed as a further subclass of the accessing class should still reach the protected member");
    }

    @Test
    public void nullReceiverTypeSkipsTheRefinementForUnqualifiedOrStaticAccess() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference base = ts.tryResolve(ProtectedBase.class);
        JClassReference sub1 = ts.tryResolve(ProtectedSub1.class);

        assertTrue(base.accessFrom(sub1, null).canAccess(JAccess.PROTECTED));
        assertTrue(base.accessFrom(sub1).canAccess(JAccess.PROTECTED),
                "the one-arg overload must remain backward compatible with no refinement applied");
    }
}
