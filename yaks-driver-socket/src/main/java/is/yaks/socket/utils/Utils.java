package is.yaks.socket.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.socket.types.MessageImpl;

public class Utils {

    public static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static int max_buffer_size = (64 * 1024);

    private Utils() {
        // nothing to do, just hides the implicit public one
    }

    public static void fail(String string) {
        try {
            throw new RuntimeException(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String stringify(String text) {
        return "_" + text.replaceAll("\\-", "_");
    }

    // returns a positive long number 64 bits
    public static long generate_correlation_id() {
        Random random = new Random();
        return Math.abs(random.nextLong());
    }

    public static ByteBuffer mapToByteBuffer(Map<String, String> map) {
        ByteBuffer buffer = ByteBuffer.allocate(max_buffer_size);
        buffer.put(VLEEncoder.encode(map.size())); // put the size of the properties map

        Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            buffer.put(VLEEncoder.encode(entry.getKey().length()));
            buffer.put(entry.getKey().getBytes());
            buffer.put(VLEEncoder.encode(entry.getValue().length()));
            buffer.put(entry.getValue().getBytes());
        }
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer mapDataToByteBuffer(Map<String, String> map) {
        ByteBuffer buffer = ByteBuffer.allocate(max_buffer_size);
        buffer.put(VLEEncoder.encode(map.size())); // put the size of the properties map

        Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            buffer.put(VLEEncoder.encode(entry.getKey().length()));
            buffer.put(entry.getKey().getBytes());
            buffer.put((byte) Encoding.RAW.getIndex()); // Encoding.RAW = 0x01
            buffer.put(VLEEncoder.encode(entry.getValue().length()));
            buffer.put(entry.getValue().getBytes());
        }
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer porpertiesListToByteBuffer(Map<String, String> propertiesList) {
        ByteBuffer buf_vle = ByteBuffer.allocate(MessageImpl.vle_bytes);
        ByteBuffer buf_prop = ByteBuffer.allocate(max_buffer_size);

        for (Map.Entry<String, String> entry : propertiesList.entrySet()) {

            buf_prop.put(entry.getKey().getBytes());
            buf_prop.put("=".getBytes());
            buf_prop.put(entry.getValue().getBytes());
        }

        // calculate the property length
        buf_prop.flip();
        buf_vle.put(VLEEncoder.encode(buf_prop.limit()));
        buf_vle.flip();

        ByteBuffer buff = ByteBuffer.allocate(buf_vle.limit() + buf_prop.limit());
        buff.put(buf_vle);
        buff.put(buf_prop);
        buff.flip();
        return buff;
    }

    public static ByteBuffer valuesToByteBuffer(Map<Path, Value> kvs) {
        // ByteBuffer buf_vle = ByteBuffer.allocate(MessageImpl.vle_bytes);
        ByteBuffer buf_vals = ByteBuffer.allocate(max_buffer_size);

        for (Map.Entry<Path, Value> entry : kvs.entrySet()) {
            buf_vals.put((byte) Encoding.RAW.getIndex());
            buf_vals.put((byte) entry.getKey().toString().length());
            buf_vals.put(entry.getKey().toString().getBytes());
            buf_vals.put((byte) Encoding.STRING.getIndex());
            buf_vals.put((byte) entry.getValue().getValue().toString().length());
            buf_vals.put(entry.getValue().getValue().toString().getBytes());
        }

        // calculate the vals length
        buf_vals.flip();
        // buf_vle.put(VLEEncoder.encode(buf_vals.limit()));
        // buf_vle.flip();
        //
        // ByteBuffer buff = ByteBuffer.allocate(buf_vals.limit());
        // buff.put(buf_vle);
        // buff.put(buf_vals);
        // buff.flip();
        return buf_vals;
    }

    public static ByteBuffer workspaceListToByteBuffer(Map<Path, Value> wkspList) {
        ByteBuffer buffer = ByteBuffer.allocate(max_buffer_size);
        buffer.put(VLEEncoder.encode(wkspList.size())); // put the size of the map i.e. 0x01

        for (Map.Entry<Path, Value> entry : wkspList.entrySet()) {
            Path key = (Path) entry.getKey();
            Value val = (Value) entry.getValue();
            String v = val.getValue().toString();
            if (!val.getEncoding().equals(Encoding.JSON)) {
                if (v.contains("{") && v.contains("}")) {
                    v = v.substring(v.indexOf("{") + 1, v.indexOf("}"));
                }
            }

            buffer.put(VLEEncoder.encode(key.toString().length())); // CHANGED: it has to be a vle because the key can
                                                                    // grow to much

            buffer.put(key.toString().getBytes());
            buffer.put((byte) val.getEncoding().getIndex());
            if (val.getEncoding().getIndex() == Encoding.RAW.getIndex()) {
                buffer.put((byte) val.getEncoding().getIndex());
                buffer.put(Encoding.get_raw_format().getBytes());
            }
            buffer.put(VLEEncoder.encode(v.length())); // CHANGED this too

            buffer.put(v.getBytes());
        }
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer listDataToByteBuffer(List<String> data) {
        ByteBuffer buffer = ByteBuffer.allocate(max_buffer_size);
        Iterator<String> entries = data.iterator();
        while (entries.hasNext()) {
            String entry = entries.next();
            buffer.put(VLEEncoder.encode(entry.length()));
            buffer.put(entry.getBytes());
        }
        buffer.flip();
        return buffer;
    }

    public static String decodeByteArrayToString(byte[] msg) {
        byte[] msg_ = new byte[msg.length];

        for (int i = 0; i < msg.length; i++) {
            msg_[i] = (byte) (msg[i] & 0xFF);
        }
        return new String(Base64.getDecoder().decode(msg_));
    }

    public static byte[] toByteArray(int value) {
        return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
    }

    public static String printHex(byte[] bytes) {
        String val = "";
        for (int i = 0; i < bytes.length; i++)
            val += Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ";
        return val;
    }
}
