package honeyroasted.jype.basic;

import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JSignatureParser;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.visitor.JTypeVisitors;

public class SignatureTest<T> {

    private static class Foo<T> {

        private class Bar {

            private class Baz<K> {

            }

        }

    }

    public static void main(String[] args) {
        JSignatureParser parser = new JSignatureParser("<T::Lhoneyroasted/jype/system/solver/operations/FindLeastUpperBoundTests$Foo<TT;>;>Ljava/lang/Object;");
        JSignature sig = parser.parseClassDeclaration();
        System.out.println(sig);

        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;
        JSignature signature = JTypeVisitors.TO_SIGNATURE.visit(system.tryResolve(new JTypeToken<Foo<String>.Bar.Baz<Integer>>() {}));
        System.out.println(signature);

        try {
            System.out.println(system.tryResolve(SignatureTest.class.getMethod("getIt", Object.class)).signature());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        System.out.println(system.constants().voidType().descriptor().toString());
    }

    public <Z> Z getIt(Z val) {
        return val;
    }

}
