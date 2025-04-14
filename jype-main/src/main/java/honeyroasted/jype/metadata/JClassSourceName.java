package honeyroasted.jype.metadata;

public record JClassSourceName(String name) {

    @Override
    public String toString() {
        return this.name;
    }

}
