package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Test {

    public static void main(String[] args) {
        test();
    }

    public static <T extends B, B extends List<T>> void test() {
        TypeSystem system = new TypeSystem();

        ParameterizedClassType t1 = (ParameterizedClassType) system.resolve(new TypeToken<LinkedHashMap<String, Integer>>() {}).get();
        ClassType t2 = (ClassType) system.resolve(new TypeToken<Map>() {}).get();
        ClassType t3 = (ClassType) system.resolve(new TypeToken<HashMap>() {}).get();

        System.out.println(t1.relativeSupertype(t2.classReference()));
        System.out.println(t1.relativeSupertype(t3.classReference()));
        System.out.println();
    }

}
