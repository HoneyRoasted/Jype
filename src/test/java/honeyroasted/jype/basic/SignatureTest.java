package honeyroasted.jype.basic;

import honeyroasted.jype.metadata.signature.JSignature;
import honeyroasted.jype.metadata.signature.JSignatureParser;

public class SignatureTest {

    public static void main(String[] args) {
        JSignatureParser parser = new JSignatureParser("<T::Lhoneyroasted/jype/system/solver/operations/FindLeastUpperBoundTests$Foo<TT;>;>Ljava/lang/Object;");
        JSignature sig = parser.parseClassDeclaration();
        System.out.println(sig);
    }

}
