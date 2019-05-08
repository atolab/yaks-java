package is.yaks.socket.async;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.types.Message;
import is.yaks.socket.types.MessageCode;
import is.yaks.socket.types.MessageFactory;
import is.yaks.socket.types.MessageImpl;
import is.yaks.socket.utils.ByteBufferPoolImpl;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;

public class AsyncYaksRuntimeImpl implements AsyncYaksRuntime {

    int vle_bytes = 8;

    int max_buffer_size = (64 * 1024);

    private SocketChannel socket;
    private HashMap<Integer, Message> workingMap;
    private HashMap<String, Message> listenersMap;
    private HashMap<Path, Message> evalsMap;
    private ByteBufferPoolImpl bytePool;

    public SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public HashMap<Integer, Message> getWorkingMap() {
        return workingMap;
    }

    public void setWorkingMap(HashMap<Integer, Message> workingMap) {
        this.workingMap = workingMap;
    }

    private Map<String, String> propertiesList;

    private static AsyncYaksRuntimeImpl instance;

    private AsyncYaksRuntimeImpl() {
    }

    public static synchronized AsyncYaksRuntimeImpl getInstance() {
        if (instance == null) {
            instance = new AsyncYaksRuntimeImpl();
        }
        return instance;
    }

    @Override
    public CompletableFuture<Yaks> create(Properties properties) {
        return null;
    }

    @Override
    public CompletableFuture<Void> destroy(Yaks async_yaks) {
        return null;
    }

    public ByteBuffer write(SocketChannel sock, Message msg) {

        ByteBuffer buf_vle = ByteBuffer.allocate(vle_bytes);
        ByteBuffer buf_msg = ByteBuffer.allocate(max_buffer_size);
        try {

            buf_msg.put((byte) msg.getMessageCode().getValue());
            buf_msg.put((byte) msg.getFlags());
            buf_msg.put(VLEEncoder.encode(msg.getCorrelationID()));
            if (!msg.getPropertiesList().isEmpty()) {
                buf_msg.put(Utils.porpertiesListToByteBuffer(msg.getPropertiesList()));
            }
            if (!msg.getWorkspaceList().isEmpty()) {
                buf_msg.put(Utils.workspaceListToByteBuffer(msg.getWorkspaceList()));
            }
            if (msg.getPath() != null) {
                buf_msg.put(VLEEncoder.encode(msg.getPath().toString().length()));
                buf_msg.put(msg.getPath().toString().getBytes());
            }
            if (msg.getSelector() != null) {
                buf_msg.put(VLEEncoder.encode(msg.getSelector().toString().length()));
                buf_msg.put(msg.getSelector().toString().getBytes());
            }
            // adding the msg length
            buf_msg.flip();
            buf_vle.put(VLEEncoder.encode(buf_msg.limit()));

            if (sock.isConnected()) {
                sock.write((ByteBuffer) buf_vle.flip());
                sock.write(buf_msg);
                workingMap.put(msg.getCorrelationID(), msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return buf_msg;
    }

    public Message read(ByteBuffer buffer) {

        Value value;
        String strKey = "", strValue = "";
        Message msg = new MessageImpl();

        buffer.flip();
        int msgCode = ((int) buffer.get() & 0xFF);
        switch (msgCode) {
        case 0xD0:
            msg = new MessageFactory().getMessage(MessageCode.OK, null);
            break;
        case 0xD1:
            msg = new MessageFactory().getMessage(MessageCode.VALUES, null);
            break;
        case 0xB2:
            msg = new MessageFactory().getMessage(MessageCode.NOTIFY, null);
            break;
        case 0xE0:
            msg = new MessageFactory().getMessage(MessageCode.ERROR, null);
            break;
        default:
            break;
        }

        propertiesList = new HashMap<String, String>();

        msg.setFlags((int) buffer.get()); // get the flags of the msg

        msg.setCorrelationId(VLEEncoder.read_correlation_id(buffer)); // get the correlation_id (vle)

        if ((msgCode == 0xD0) || (msgCode == 0xB2)) {
            if (msg.getFlags() == 1) {
                int length_properties = (int) buffer.get(); // get length_properties
                if (length_properties > 0) {
                    byte[] key_bytes = new byte[length_properties];
                    buffer.get(key_bytes, 0, length_properties);
                    try {
                        strValue = new String(key_bytes, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    strKey = strValue.substring(0, strValue.indexOf("="));
                    strValue = strValue.substring(strValue.indexOf("=") + 1);
                    propertiesList.put(strKey, strValue);
                    msg.add_property(strKey, strValue);
                }
            }
        }
        if (msgCode == 0xD1) {
            int valFormat = (buffer.get() & 0xFF); // read 0x81 = 129
            buffer.get(); // reads out the 0x00
            if (valFormat > 0x80) {
                while (buffer.hasRemaining()) {
                    int length_key = (int) buffer.get();
                    byte[] key_bytes = new byte[length_key];
                    buffer.get(key_bytes, 0, length_key);

                    int val_encoding = (int) buffer.get();
                    if (val_encoding == 0x01) {
                        byte[] val_format = new byte[2]; // gets the 0x01 0x20
                        buffer.get(val_format, 0, 2);
                    }
                    int length_value = (int) buffer.get();
                    byte[] value_bytes = new byte[length_value];
                    buffer.get(value_bytes, 0, length_value);

                    try {
                        strKey = new String(key_bytes, "UTF-8");
                        strValue = new String(value_bytes, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    value = new Value();
                    value.setValue(strValue);
                    value.setEncoding(Encoding.getEncoding(val_encoding));
                    msg.add_value(Path.ofString(strKey), value);
                }
            }
        }
        return msg;
    }

    @Override
    public CompletableFuture<Void> process_login(Properties properties, Yaks yaks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Void> process_logout(Yaks yaks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Workspace> process_workspace(Path path, Yaks yaks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Map<Path, Value>> process_get(Properties properties, YSelector yselector, Yaks yaks,
            int quorum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Boolean> process_put(Properties properties, Path path, Value val, Yaks yaks, int quorum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Boolean> process_update(Properties properties, Path path, Value val, Yaks yaks,
            int quorum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Boolean> process_remove(Properties properties, Yaks yaks, int quorum) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<String> process_subscribe(Properties properties, YSelector yselector, Yaks yaks,
            Listener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Boolean> process_unsubscribe(String subid, Yaks yaks) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Void> process_register_eval(Properties properties, Path path, Yaks yaks, Path workpath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Void> process_unregister_eval(Properties properties, Path path, Yaks yaks, Path workpath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Map<Path, Value>> process_eval(Properties properties, YSelector yselector, Yaks yaks,
            int multiplicity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Void> process() {
        return null;
    }

}
