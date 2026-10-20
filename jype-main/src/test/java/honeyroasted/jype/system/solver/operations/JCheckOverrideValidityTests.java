package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JCheckOverrideValidityTests {

    static class ValidBase {
        public Number m(String s) throws IOException {
            return null;
        }
    }

    static class ValidSub {
        public Integer m(String s) throws FileNotFoundException {
            return null;
        }
    }

    static class DifferentSignatureHost {
        public void other(int i) {
        }
    }

    static class StaticHost {
        public static void m() {
        }
    }

    static class InstanceHost {
        public void m() {
        }
    }

    static class PrivateHost {
        private void m() {
        }
    }

    static class FinalHost {
        public final void m() {
        }
    }

    static class NormalHost {
        public void m() {
        }
    }

    static class ProtectedAccessHost {
        protected void m() {
        }
    }

    static class ReducedAccessHost {
        void m() {
        }
    }

    static class ReturnHost {
        public Number m() {
            return null;
        }
    }

    static class CovariantReturnHost {
        public Integer m() {
            return null;
        }
    }

    static class UnrelatedReturnHost {
        public String m() {
            return null;
        }
    }

    static class BroaderThrowsHost {
        public void m() throws Exception {
        }
    }

    static class UncheckedThrowsHost {
        public void m() throws RuntimeException {
        }
    }

    private static JMethodReference method(JTypeSystem ts, Class<?> cls, String name) {
        JClassReference ref = ts.tryResolve(cls);
        return ref.declaredMethods().stream()
                .filter(m -> m.location().name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void validOverrideWithCovariantReturnAndNarrowerThrowsIsValid() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, ValidSub.class, "m");
        JMethodReference overridden = method(ts, ValidBase.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertTrue(result.valid(), () -> "expected valid override, got " + result.reason());
    }

    @Test
    public void differentSignatureIsNotOverrideEquivalent() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, DifferentSignatureHost.class, "other");
        JMethodReference overridden = method(ts, ValidBase.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.NOT_OVERRIDE_EQUIVALENT, result.reason());
    }

    @Test
    public void staticInstanceMismatchIsHidingNotOverriding() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, InstanceHost.class, "m");
        JMethodReference overridden = method(ts, StaticHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.HIDDEN_NOT_OVERRIDDEN, result.reason());
    }

    @Test
    public void privateMethodsCannotBeOverridden() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, NormalHost.class, "m");
        JMethodReference overridden = method(ts, PrivateHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.CANNOT_OVERRIDE_PRIVATE, result.reason());
    }

    @Test
    public void finalMethodsCannotBeOverridden() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, NormalHost.class, "m");
        JMethodReference overridden = method(ts, FinalHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.CANNOT_OVERRIDE_FINAL, result.reason());
    }

    @Test
    public void overrideCannotReduceAccess() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, ReducedAccessHost.class, "m");
        JMethodReference overridden = method(ts, ProtectedAccessHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.ACCESS_REDUCED, result.reason());
    }

    @Test
    public void unrelatedReturnTypeIsNotSubstitutable() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, UnrelatedReturnHost.class, "m");
        JMethodReference overridden = method(ts, ReturnHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.RETURN_TYPE_NOT_SUBSTITUTABLE, result.reason());
    }

    @Test
    public void covariantReturnTypeIsSubstitutable() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, CovariantReturnHost.class, "m");
        JMethodReference overridden = method(ts, ReturnHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertTrue(result.valid(), () -> "expected valid override, got " + result.reason());
    }

    @Test
    public void broaderCheckedThrowsClauseIsIncompatible() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, BroaderThrowsHost.class, "m");
        JMethodReference overridden = method(ts, NormalHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.THROWS_CLAUSE_INCOMPATIBLE, result.reason());
    }

    @Test
    public void uncheckedThrowsClauseIsAlwaysCompatible() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, UncheckedThrowsHost.class, "m");
        JMethodReference overridden = method(ts, NormalHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertTrue(result.valid(), () -> "expected valid override, got " + result.reason());
    }
}
