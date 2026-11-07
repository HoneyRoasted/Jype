package honeyroasted.jype.fuzz;

import java.util.List;

public record ExpressionWorld(String packageName, String header, List<String> declarations, List<Probe> probes) {
}
