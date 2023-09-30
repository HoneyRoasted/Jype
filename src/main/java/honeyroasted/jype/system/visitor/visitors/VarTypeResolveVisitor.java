package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.function.Function;

public class VarTypeResolveVisitor extends TypeVisitors.StructuralMapping<Void> {
    private Function<VarType, Type> resolver;

    public VarTypeResolveVisitor(Function<VarType, Type> resolver) {
        this.resolver = resolver;
    }

    @Override
    public Type visitTypeVar(VarType type, Void context) {
        return this.resolver.apply(type);
    }
}
