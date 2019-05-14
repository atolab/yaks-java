package is.yaks;

public interface Observer {
    // public void onData(Map<Path,Value> map);
    //
    // public void onData(Path key, Value value);

    public void onPut(Path key, Value value);

    public void onUpdate(Path key, Value value);

    public void onRemove(Path key, Value value);
}