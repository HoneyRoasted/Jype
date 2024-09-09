package honeyroasted.jype.location;

public record JClassBytecode(byte[] bytes) {

    @Override
    public String toString() {
        return "<raw binary>";
    }
}
