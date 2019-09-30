package is.yaks;

public enum Encoding {

    RAW (RawValue.Decoder.getEncodingFlag(), RawValue.Decoder),
    STRING (StringValue.Decoder.getEncodingFlag(), StringValue.Decoder),
    PROPERTIES (PropertiesValue.Decoder.getEncodingFlag(), PropertiesValue.Decoder),
    JSON((short)0x04, StringValue.Decoder);
    // SQL((short) 0x05);

    private short flag;
    private Value.Decoder decoder;

    private Encoding(short flag, Value.Decoder decoder) {
        this.flag = flag;
        this.decoder = decoder;
    }

    public short getFlag() {
        return flag;
    }

    public Value.Decoder getDecoder() {
        return decoder;
    }


    protected static Encoding fromFlag(short flag) throws YException {
        if (flag == RAW.getFlag()) {
            return RAW;
        }
        else if (flag == STRING.getFlag()) {
            return STRING;
        }
        else if (flag == PROPERTIES.getFlag()) {
            return PROPERTIES;
        }
        else if (flag == JSON.getFlag()) {
            return JSON;
        }
        else {
            throw new YException("Unkown encoding flag: "+flag);
        }
    }

}
