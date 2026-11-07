package honeyroasted.jype.fuzz;

import java.util.List;

public record GeneratedClass(String name, boolean isInterface, boolean isFinal, List<String> typeParameters) {
    public int arity() {
        return this.typeParameters.size();
    }
}
