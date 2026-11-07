package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

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

    static class ListString {
        void m(List<String> l) {
        }
    }

    static class ListInteger {
        void m(List<Integer> l) {
        }
    }

    @SuppressWarnings("rawtypes")
    static class ListRaw {
        void m(List l) {
        }
    }

    static class GenericId {
        <T> T id(T t) {
            return t;
        }

        <T extends Number> void g(T t) {
        }

        <T> void p(T t) {
        }

        <T> List<T> r() {
            return null;
        }

        List<String> s() {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    static class GenericIdOverride {
        <U> U id(U u) {
            return u;
        }

        <T extends Integer> void g(T t) {
        }

        void p(Object t) {
        }

        List r() {
            return null;
        }

        List s() {
            return null;
        }
    }

    static class ObjectParam {
        void p(Object t) {
        }
    }

    static class GenericParam {
        <T> void p(T t) {
        }
    }

    static class StaticObject {
        static Object m() {
            return null;
        }

        static void v() {
        }

        static final void f() {
        }
    }

    static class StaticString {
        static String m() {
            return null;
        }

        static int v() {
            return 0;
        }

        static void f() {
        }
    }

    static class SamePackageName {
        int pp() {
            return 0;
        }
    }

    private static JCheckOverrideValidity.Reason check(JTypeSystem ts, Class<?> override, Class<?> overridden, String name) {
        return ts.operations().checkOverrideValidity(method(ts, override, name), method(ts, overridden, name)).reason();
    }

    @Test
    public void overridingRulesMatchJavac() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        assertEquals(JCheckOverrideValidity.Reason.NAME_CLASH, check(ts, ListInteger.class, ListString.class, "m"), "m(List<Integer>) vs m(List<String>)");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, ListRaw.class, ListString.class, "m"), "m(List) overrides m(List<String>)");
        assertEquals(JCheckOverrideValidity.Reason.NAME_CLASH, check(ts, ListString.class, ListRaw.class, "m"), "m(List<String>) vs m(List)");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, GenericIdOverride.class, GenericId.class, "id"), "<U> U id(U) overrides <T> T id(T)");
        assertEquals(JCheckOverrideValidity.Reason.NOT_OVERRIDE_EQUIVALENT, check(ts, GenericIdOverride.class, GenericId.class, "g"), "<T extends Integer> g vs <T extends Number> g");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, GenericIdOverride.class, GenericId.class, "p"), "p(Object) overrides <T> p(T)");
        assertEquals(JCheckOverrideValidity.Reason.NAME_CLASH, check(ts, GenericParam.class, ObjectParam.class, "p"), "<T> p(T) vs p(Object)");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, GenericIdOverride.class, GenericId.class, "r"), "List r() overrides <T> List<T> r()");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, GenericIdOverride.class, GenericId.class, "s"), "List s() overrides List<String> s()");
        assertEquals(JCheckOverrideValidity.Reason.STATIC_HIDES_INSTANCE, check(ts, StaticHost.class, InstanceHost.class, "m"), "static m() vs m()");
        assertEquals(JCheckOverrideValidity.Reason.VALID, check(ts, StaticString.class, StaticObject.class, "m"), "static String m() hides static Object m()");
        assertEquals(JCheckOverrideValidity.Reason.RETURN_TYPE_NOT_SUBSTITUTABLE, check(ts, StaticString.class, StaticObject.class, "v"), "static int v() vs static void v()");
        assertEquals(JCheckOverrideValidity.Reason.CANNOT_OVERRIDE_FINAL, check(ts, StaticString.class, StaticObject.class, "f"), "static f() vs static final f()");
        assertEquals(JCheckOverrideValidity.Reason.NOT_INHERITED, ts.operations().checkOverrideValidity(method(ts, SamePackageName.class, "pp"),
                method(ts, honeyroasted.jype.system.solver.fixtures.packageaccess.PackageHost.class, "pp")).reason(), "int pp() vs package-private pp() in another package");
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
    public void instanceMethodCannotOverrideStaticMethod() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JMethodReference override = method(ts, InstanceHost.class, "m");
        JMethodReference overridden = method(ts, StaticHost.class, "m");

        JCheckOverrideValidity.Result result = ts.operations().checkOverrideValidity(override, overridden);
        assertFalse(result.valid());
        assertEquals(JCheckOverrideValidity.Reason.INSTANCE_OVERRIDES_STATIC, result.reason());
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
