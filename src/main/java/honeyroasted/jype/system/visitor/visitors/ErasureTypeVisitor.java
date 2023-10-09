package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.*;

public class ErasureTypeVisitor implements TypeVisitors.StructuralMapping<Boolean> {

    @Override
    public Type visitWildcardType(WildType type, Boolean recurse) {
        return visit(type.upperBounds().iterator().next(), recurse);
    }

    @Override
    public Type visitClassType(ClassType type, Boolean recurse) {
        if (type instanceof ParameterizedClassType pType) {
            ClassReference ref = pType.classReference();
            if (!recurse) {
                ParameterizedClassType ct = ref.parameterized();
                ct.setUnmodifiable(false);
                ct.setOuterType(pType.outerType());
                ct.setUnmodifiable(true);
                return ct;
            }
            return ref;
        }
        return type;
    }

    @Override
    public Type visitVarType(VarType type, Boolean recurse) {
        return visit(type.upperBounds().isEmpty() ? type.typeSystem().constants().object() : type.upperBounds().iterator().next(), recurse);
    }
}
