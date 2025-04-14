package honeyroasted.jype.metadata;

public record JClassBytecode(byte[] bytes) {

    @Override
    public String toString() {
        return "<raw binary>";
    }
}
