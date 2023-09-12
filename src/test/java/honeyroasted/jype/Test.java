package honeyroasted.jype;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.system.TypeSystem;

public class Test {

    public static void main(String[] args) {
        test();
    }

    public static <T> void test() {
        TypeSystem system = new TypeSystem();
        System.out.println(system.resolve(ClassLocation.of(int.class)).get());
    }

}
