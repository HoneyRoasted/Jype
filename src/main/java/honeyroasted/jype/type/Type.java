package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

public sealed interface Type permits honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType, honeyroasted.jype.modify.AbstractType {

    TypeSystem typeSystem();

    <R, P> R accept(TypeVisitor<R, P> visitor, P context);

}
