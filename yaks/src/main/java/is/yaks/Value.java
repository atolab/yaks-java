package is.yaks;

import java.nio.ByteBuffer;

public interface Value {

    public interface Decoder {
        public short getEncodingFlag();

        public Value decode(ByteBuffer buf);
    }

    public Encoding getEncoding();

    public ByteBuffer encode();
}
