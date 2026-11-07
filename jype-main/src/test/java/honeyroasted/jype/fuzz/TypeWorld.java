package honeyroasted.jype.fuzz;

import java.util.List;

public record TypeWorld(String packageName, String source, List<String> fieldNames, List<String> fieldTypes) {
    public String holderName() {
        return this.packageName + ".H";
    }
}
