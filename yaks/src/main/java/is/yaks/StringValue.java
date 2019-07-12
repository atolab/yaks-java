package is.yaks;

import java.nio.charset.Charset;
import java.nio.ByteBuffer;

public class StringValue implements Value{

    private static final Charset utf8 = Charset.forName("UTF-8");

    private String s;


    public StringValue(String s) {
        this.s = s;
    }

    public short getEncoding() {
        return 0x02;
    }

    public ByteBuffer encode() {
        return ByteBuffer.wrap(s.getBytes(utf8));
    }

    public String toString() {
        return s;
    }

    public static final Value.Decoder Decoder = new Value.Decoder() {
        public Value decode(ByteBuffer buf) {
            return new StringValue(new String(buf.array(), utf8));
        }
    };

}