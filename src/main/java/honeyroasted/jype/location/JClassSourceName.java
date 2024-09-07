package honeyroasted.jype.location;

public record JClassSourceName(String name) {

    @Override
    public String toString() {
        return this.name;
    }

}
