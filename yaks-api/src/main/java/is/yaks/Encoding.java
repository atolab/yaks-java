package is.yaks;

public enum Encoding {

    RAW(0x01), STRING(0x02), JSON(0x03), PROTOBUF(0x04), SQL(0x05), PROPERTY(0x06), INVALID(0x00);

    static String raw_format = " ";
    static String values_format = "^";

    private int index;

    private Encoding(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static Encoding getEncoding(int encodingIndex) {
        for (Encoding e : Encoding.values()) {
            if (e.index == encodingIndex)
                return e;
        }
        throw new IllegalArgumentException("Encoding not found.");
    }

    public static String get_raw_format() {
        return raw_format;
    }

    public static String get_values_format() {
        return values_format;
    }
}
