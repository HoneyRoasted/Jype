package honeyroasted.jype;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.system.visitor.visitors.ToSignatureTypeVisitor;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Test<T> {

    public static <T> void main(String[] args) {
        List<TypeToken> tokens = List.of(
                new TypeToken<List<T>>() {},
                new TypeToken<String>() {},
                new TypeToken<Map<List<T>, Integer>>() {},
                new TypeToken<List<? extends T>>() {}
        );

        for (TypeToken t : tokens) {
            Type type = t.resolve();
            System.out.println(t.extractType() + " = " + TypeVisitors.TO_SIGNATURE.visit(type, ToSignatureTypeVisitor.Mode.USAGE));
        }
    }

}
