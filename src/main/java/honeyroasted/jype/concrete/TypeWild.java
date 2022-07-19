package honeyroasted.jype.concrete;

import java.util.Optional;

public class TypeWild implements TypeConcrete {
    private TypeConcrete upperBound;
    private TypeConcrete lowerBound;

    private TypeWild(TypeConcrete upperBound, TypeConcrete lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public static TypeWild withUpper(TypeConcrete upperBound) {
        return new TypeWild(upperBound, null);
    }

    public static TypeWild withLower(TypeConcrete lowerBound) {
        return new TypeWild(null, lowerBound);
    }

    public Optional<TypeConcrete> upperBound() {
        return Optional.ofNullable(this.upperBound);
    }

    public Optional<TypeConcrete> lowerBound() {
        return Optional.ofNullable(this.lowerBound);
    }

}
