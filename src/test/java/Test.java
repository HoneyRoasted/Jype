import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.concrete.TypePlaceholder;
import honeyroasted.jype.system.TypeSystem;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = new TypeSystem();

        TypePlaceholder inferable = system.newPlaceholder();
        TypeConcrete listA = system.declaration(List.class).withArguments(inferable);
        TypeConcrete listB = system.declaration(List.class).withArguments(system.of(Integer.class));

        System.out.println(listA.assignabilityTo(listB).simplify());
        System.out.println(listA.isAssignableTo(listB));
    }

}
