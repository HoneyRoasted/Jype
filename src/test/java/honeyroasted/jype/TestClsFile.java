package honeyroasted.jype;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.resolver.binary.JBinaryClassFinder;
import org.glavo.classfile.Attributes;
import org.glavo.classfile.ClassFile;
import org.glavo.classfile.ClassModel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

public class TestClsFile {

    public static void main(String[] args) throws IOException {
        JBinaryClassFinder finder = new JBinaryClassFinder.Dir(Paths.get("build/classes/java/main"));
        Optional<byte[]> binaryOpt = finder.locate(JClassLocation.of(JBinaryClassFinder.class));
        if (binaryOpt.isPresent()) {
            byte[] binary = binaryOpt.get();
            ClassModel model = ClassFile.of().parse(binary);
            model.findAttribute(Attributes.NEST_MEMBERS).get()
                    .nestMembers().forEach(System.out::println);
        }
    }

}
