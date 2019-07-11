package is.yaks;

import java.nio.ByteBuffer;

public interface Value {

    public interface Decoder {
        public Value decode(ByteBuffer buf);
    }

    public short getEncoding();

    public ByteBuffer encode();
}
