package is.yaks;

import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;


public class PropertiesValue implements Value{

    private static final short ENCODING_FLAG = 0x03;

    private static final Charset utf8 = Charset.forName("UTF-8");

    private Properties p;


    public PropertiesValue(Properties p) {
        this.p = p;
    }

    public Properties getProperties() {
        return p;
    }

    public Encoding getEncoding() {
        return Encoding.PROPERTIES;
    }

    public ByteBuffer encode() {
        return ByteBuffer.wrap(toString().getBytes(utf8));
    }

    private static final String PROP_SEP = ";";
    private static final String KV_SEP   = "=";

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        int i=0;
        for (Map.Entry<Object, Object> e : p.entrySet()) {
            buf.append(e.getKey()).append(KV_SEP).append(e.getValue());
            if (++i < p.size()) {
                buf.append(PROP_SEP);
            }
        }
        return buf.toString();
    }

    public static final Value.Decoder Decoder = new Value.Decoder() {

        public short getEncodingFlag() {
            return ENCODING_FLAG;
        }

        private Properties fromString(String s) {
            Properties p = new Properties();
            if (s.length() > 0) {
                for (String kv : s.split(PROP_SEP)) {
                    int i = kv.indexOf(KV_SEP);
                    if (i < 0) {
                        p.setProperty(kv, "");
                    } else {
                        p.setProperty(kv.substring(0, i), kv.substring(i+1));
                    }
                }
            }
            return p;
        }

        public Value decode(ByteBuffer buf) {
            return new PropertiesValue(fromString(new String(buf.array(), utf8)));
        }
    };

}
