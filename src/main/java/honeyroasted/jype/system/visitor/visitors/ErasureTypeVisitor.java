package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.*;

public class ErasureTypeVisitor implements TypeVisitors.StructuralMapping<Boolean> {

    @Override
    public Type visitWildcardType(WildType type, Boolean recurse) {
        return visit(wildUpperBound(type), recurse);
    }

    private static Type wildUpperBound(Type t) {
        if (t instanceof WildType wType) {
            return wildUpperBound(wType.upperBounds().iterator().next());
        } else {
            return t;
        }
    }

    @Override
    public Type visitClassType(ClassType type, Boolean recurse) {
        if (type instanceof ParameterizedClassType pType) {
            return pType.classReference();
        }
        return type;
    }

    @Override
    public Type visitVarType(VarType type, Boolean recurse) {
        return visit(type.upperBounds().isEmpty() ? type.typeSystem().constants().object() : type.upperBounds().iterator().next(), recurse);
    }
}
