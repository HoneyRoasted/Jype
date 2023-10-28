package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.CompatibilityTypeSolver;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.Type;

import java.util.List;

public class Test {

    public static <T extends List<T>> void main(String[] args) {
        Type varType = new TypeToken<T>() {}.resolve();
        varType = TypeVisitors.VAR_WIDLCARDER.visit(varType);

        Type tst = ((ClassReference) new TypeToken<List>(){}.resolve()).parameterized((ArgumentType) varType);
        Type foo = new TypeToken<Foo>() {}.resolve();

        System.out.println(new CompatibilityTypeSolver()
                .bind(new TypeBound.Compatible(foo, tst))
                .solve(TypeSystem.RUNTIME).toString(true));
    }

}
