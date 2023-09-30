package honeyroasted.jype.type;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

public sealed interface Type permits AbstractPossiblyUnmodifiableType, AbstractType, TypeWithMetadata, ClassType, MethodType {

    TypeSystem typeSystem();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

}
