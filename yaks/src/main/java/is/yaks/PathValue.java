package is.yaks;

public class PathValue {

    private Path path;
    private Value value;

    protected PathValue(Path path, Value value) {
        this.path = path;
        this.value = value;
    }

    public Path getPath() {
        return path;
    }

    public Value getValue() {
        return value;
    }
}