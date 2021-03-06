package is.yaks;

import io.zenoh.Timestamp;

/**
 * The notification of a change for a path/value in Yaks.
 * See {@link Listener}.
 */
public class Change {

    public enum Kind {
        PUT( 0x00),
        UPDATE( 0x01),
        REMOVE( 0x02);

        private int numVal;

        private Kind(int numVal) {
            this.numVal = numVal;
        }

        public int value() {
            return numVal;
        }

        protected static Kind fromInt(int numVal) throws YException {
            if (numVal == PUT.value()) {
                return PUT;
            }
            else if (numVal == UPDATE.value()) {
                return UPDATE;
            }
            else if (numVal == REMOVE.value()) {
                return REMOVE;
            }
            else {
                throw new YException("Unkown change kind value: "+numVal);
            }
        }
    }

    private Path path;
    private Kind kind;
    private Timestamp timestamp;
    private Value value;

    protected Change(Path path, Kind kind, Timestamp timestamp, Value value) {
        this.path = path;
        this.kind = kind;
        this.timestamp = timestamp;
        this.value = value;
    }

    public Path getPath() {
        return path;
    }

    public Kind getKind() {
        return kind;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
    public Value getValue() {
        return value;
    }

}