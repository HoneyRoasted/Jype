package honeyroasted.jype.location;

public record ClassSourceName(String name) {
    @Override
    public String toString() {
        return this.name;
    }
}
