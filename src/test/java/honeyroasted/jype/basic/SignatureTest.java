package honeyroasted.jype.basic;

import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JSignatureParser;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.visitor.JTypeVisitors;

public class SignatureTest {

    private static class Foo<T> {

        private class Bar {

            private class Baz<K> {

            }

        }

    }

    private Foo<String>.Bar.Baz<Integer> baz;

    public static void main(String[] args) {
        JSignatureParser parser = new JSignatureParser("<T::Lhoneyroasted/jype/system/solver/operations/FindLeastUpperBoundTests$Foo<TT;>;>Ljava/lang/Object;");
        JSignature sig = parser.parseClassDeclaration();
        System.out.println(sig);

        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;
        System.out.println(JTypeVisitors.TO_SIGNATURE.visit(system.tryResolve(new JTypeToken<Foo<String>.Bar.Baz<Integer>>() {})));
    }

}
