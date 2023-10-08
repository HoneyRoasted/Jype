package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.AssignabilityTypeSolver;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Test {

    public static <T> void main(String[] args) {
        TypeSystem system = new TypeSystem();
        VarType vt = system.<VarType>resolve(new TypeToken<T>() {}).get();
        Type ct1 = system.resolve(new TypeToken<T>() {}).get();
        ClassType ct2 = system.<ClassType>resolve(new TypeToken<CharSequence>() {}).get();
        ClassType ct3 = system.<ClassType>resolve(new TypeToken<String>(){}).get();

        System.out.println(new AssignabilityTypeSolver()
                .bind(new TypeBound.Subtype(ct1, ct2))
                .assume(new TypeBound.Subtype(vt, ct3)).solve(system));
    }

}
