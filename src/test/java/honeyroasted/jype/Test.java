package honeyroasted.jype;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.type.ClassReference;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) {
        test();
    }

    public static <T extends B, B extends List<T>> void test() {
        TypeSystem system = new TypeSystem();
        System.out.println(system.resolve(new TypeToken<Function<T, B>>() {}).get());
        System.out.println(system.resolve(TypeSystem.class).get());
        System.out.println(system.resolve(new MethodLocation(ClassLocation.of(Test.class), "test", ClassLocation.VOID, List.of())).get());
    }

}
