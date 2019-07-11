package is.yaks;

public enum Encoding {

    RAW((short) 0x01),
    STRING((short) 0x02),
    PROPERTIES((short) 0x03),
    JSON((short) 0x04),
    SQL((short) 0x05);

    private short numVal;

    private Encoding(short numVal) {
        this.numVal = numVal;
    }

    public short value() {
        return numVal;
    }

    protected static Encoding fromShort(short numVal) throws YException {
        if (numVal == RAW.value()) {
            return RAW;
        }
        else if (numVal == STRING.value()) {
            return STRING;
        }
        else if (numVal == PROPERTIES.value()) {
            return PROPERTIES;
        }
        else if (numVal == JSON.value()) {
            return JSON;
        }
        else {
            throw new YException("Unkown encoding value: "+numVal);
        }
    }

}
