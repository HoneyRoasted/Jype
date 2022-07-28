import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.system.TypeSystem;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = new TypeSystem();

        TypeParameter inferable = new TypeParameter.Placeholder(system.OBJECT);
        TypeConcrete listA = system.declaration(List.class).withArguments(inferable);
        TypeConcrete listB = system.declaration(List.class).withArguments(inferable);

        System.out.println(listA.assignabilityTo(listB).simplify());
        System.out.println(listA.isAssignableTo(listB));
    }

}
