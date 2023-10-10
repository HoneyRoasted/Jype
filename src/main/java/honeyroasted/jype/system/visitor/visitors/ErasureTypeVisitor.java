package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

public class ErasureTypeVisitor implements StructuralMappingVisitor<Boolean> {

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
