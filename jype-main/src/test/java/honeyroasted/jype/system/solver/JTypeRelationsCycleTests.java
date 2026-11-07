package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JTypeRelationsCycleTests {
    public interface N<Z> {
    }

    public static class Cyclic implements N<N<? super Cyclic>> {
    }

    public static class Base {
    }

    public static class Expansive<X> extends Base implements N<N<? super Expansive<Expansive<X>>>> {
    }

    public interface I<X> {
    }

    public static class Growing<Q> implements I<Growing<Growing<Q>>> {
    }

    public interface Rec<R extends Rec<R>> {
    }

    public static class RecImpl implements Rec<RecImpl> {
    }

    public static N<? super Cyclic> superCyclic;
    public static N<? extends N<? super Cyclic>> extendsSuperCyclic;
    public static N<N<? super Cyclic>> nestedCyclic;
    public static N<? super Expansive<Base>> superExpansive;
    public static Expansive<Base> expansive;
    public static I<? super Growing<String>> superGrowing;
    public static I<? extends I<?>> extendsI;
    public static Growing<String> growing;
    public static Rec<? extends Rec<?>> recWildcard;

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;

    private static JType type(String field) throws NoSuchFieldException {
        return TS.tryResolve(JTypeRelationsCycleTests.class.getField(field).getGenericType());
    }

    @Test
    @Timeout(60)
    public void cyclicSubtypingMatchesJavac() throws NoSuchFieldException {
        JType cyclic = TS.tryResolve(Cyclic.class);
        assertEquals(false, JTypeRelations.isSubtype(cyclic, type("superCyclic")), "N<? super Cyclic> x = new Cyclic()");
        assertEquals(false, TS.operations().isCompatible(cyclic, type("superCyclic"), JTypeConstraints.Compatible.Context.ASSIGNMENT), "N<? super Cyclic> x = new Cyclic() in assignment context");
        assertEquals(true, JTypeRelations.isSubtype(cyclic, type("extendsSuperCyclic")), "N<? extends N<? super Cyclic>> x = new Cyclic()");
        assertEquals(false, JTypeRelations.isCastable(type("nestedCyclic"), type("superCyclic")), "(N<? super Cyclic>) nestedCyclic");
        assertEquals(false, JTypeRelations.isSubtype(type("growing"), type("superGrowing")), "I<? super Growing<String>> x = new Growing<String>()");
        assertEquals(true, JTypeRelations.isSubtype(type("growing"), type("extendsI")), "I<? extends I<?>> x = new Growing<String>()");
        assertEquals(true, JTypeRelations.isSubtype(TS.tryResolve(RecImpl.class), type("recWildcard")), "Rec<? extends Rec<?>> x = new RecImpl()");
    }

    @Test
    @Timeout(60)
    public void expansiveSubtypingTerminates() throws NoSuchFieldException {
        assertEquals(false, JTypeRelations.isSubtype(type("expansive"), type("superExpansive")), "N<? super Expansive<Base>> x = new Expansive<Base>() overflows javac's stack");
        assertEquals(false, JTypeRelations.isCastable(type("expansive"), type("superExpansive")), "(N<? super Expansive<Base>>) new Expansive<Base>() overflows javac's stack");
        assertNotNull(TS.operations().findLeastUpperBound(new LinkedHashSet<>(List.of(type("expansive"), type("superExpansive")))), "flag ? new Expansive<Base>() : superExpansive compiles");
    }
}
