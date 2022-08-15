package honeyroasted.jype.test;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeToken;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.force.ForceResolveTypeSolver;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameterized;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CircularTest<A extends List<C>, B extends List<A>, C extends List<B>> extends TypeTest {

    @Test
    public void testDeCircle() {
        TypeConcrete a = this.typeSystem.of(new TypeToken<A>(){}).get();
        TypeConcrete b = this.typeSystem.of(new TypeToken<B>(){}).get();
        TypeConcrete c = this.typeSystem.of(new TypeToken<C>(){}).get();

        TypeConcrete la = this.typeSystem.of(new TypeToken<List<A>>(){}).get();

        assertInstanceOf(TypeOut.class, this.typeSystem.deCircularize(a));
        assertInstanceOf(TypeOut.class, this.typeSystem.deCircularize(b));
        assertInstanceOf(TypeOut.class, this.typeSystem.deCircularize(c));

        assertInstanceOf(TypeParameterized.class, this.typeSystem.deCircularize(la));
        assertInstanceOf(TypeOut.class, ((TypeParameterized) this.typeSystem.deCircularize(la)).arguments().get(0));
    }

    @Test
    public void forceResolveDoesNotError() {
        TypeConcrete a = this.typeSystem.of(new TypeToken<A>(){}).get();
        TypeConcrete b = this.typeSystem.of(new TypeToken<B>(){}).get();
        TypeConcrete c = this.typeSystem.of(new TypeToken<C>(){}).get();

        assertAll(() -> {
            new ForceResolveTypeSolver(this.typeSystem)
                    .constrain(new TypeConstraint.Bound(a, b))
                    .solve();
        }, () -> {
            new ForceResolveTypeSolver(this.typeSystem)
                    .constrain(new TypeConstraint.Bound(a, c))
                    .constrain(new TypeConstraint.Bound(b, c))
                    .constrain(new TypeConstraint.Bound(c, c))
                    .solve();
        });
    }

}
