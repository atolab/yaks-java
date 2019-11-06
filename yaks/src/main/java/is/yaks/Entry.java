package is.yaks;

import org.slf4j.LoggerFactory;

import io.zenoh.Timestamp;

public class Entry implements Comparable<Entry> {

    private Path path;
    private Value value;
    private Timestamp timestamp;

    protected Entry(Path path, Value value, Timestamp timestamp) {
        this.path = path;
        this.value = value;
        this.timestamp = timestamp;
    }

    public Path getPath() {
        return path;
    }

    public Value getValue() {
        return value;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(Entry o) {
        // Order entires according to their timestamps
        return timestamp.compareTo(o.timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (! (obj instanceof Entry))
            return false;

        Entry o = (Entry) obj;
        // As timestamp is unique per entry, only compare timestamps.
        if (!timestamp.equals(o.timestamp))
            return false;

        // however, log a warning if path and values are different...
        if (!path.equals(o.path)) {
            LoggerFactory.getLogger("is.yaks").warn(
                "INTERNAL ERROR: 2 entries with same timestamp {} have different paths: {} vs. {}",
                timestamp, path, o.path);
        }
        if (!value.equals(o.value)) {
            LoggerFactory.getLogger("is.yaks").warn(
                "INTERNAL ERROR: 2 entries with same timestamp {} have different values: {} vs. {}",
                timestamp, value, o.value);
        }

        return true;
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }
}