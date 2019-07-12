package is.yaks;

import java.nio.ByteBuffer;

public class RawValue implements Value {

    private static final short ENCODING_FLAG = 0x00;

    private ByteBuffer buf;


    public RawValue(ByteBuffer buf) {
        this.buf = buf;
    }

    public ByteBuffer getBuffer() {
        return buf;
    }

    public Encoding getEncoding() {
        return Encoding.RAW;
    }

    public ByteBuffer encode() {
        return buf;
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    public static final Value.Decoder Decoder = new Value.Decoder() {
        public short getEncodingFlag() {
            return ENCODING_FLAG;
        }

        public Value decode(ByteBuffer buf) {
            return new RawValue(buf);
        }
    };

}
