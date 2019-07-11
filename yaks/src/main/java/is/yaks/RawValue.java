package is.yaks;

import java.nio.ByteBuffer;

public class RawValue implements Value {

    private ByteBuffer buf;


    public RawValue(ByteBuffer buf) {
        this.buf = buf;
    }

    public short getEncoding() {
        return 0x00;
    }

    public ByteBuffer encode() {
        return buf;
    }

    public String toString() {
        return buf.toString();
    }

    public static final Value.Decoder Decoder = new Value.Decoder() {
        public Value decode(ByteBuffer buf) {
            return new RawValue(buf);
        }
    };

}
