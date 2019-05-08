package is.yaks.socket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.YSelector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.types.Message;
import is.yaks.socket.types.MessageCode;
import is.yaks.socket.types.MessageFactory;
import is.yaks.socket.types.MessageImpl;
import is.yaks.socket.utils.ByteBufferPoolImpl;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;

public class YaksRuntimeImpl implements YaksRuntime, Runnable {

    private static Selector selector;

    public static long DEFAULT_TIMEOUT = 1l;

    private SocketChannel socket;
    private HashMap<Integer, Message> workingMap;
    private HashMap<String, Message> listenersMap;
    private HashMap<Path, Message> evalsMap;
    private ByteBufferPoolImpl bytePool;

    public int wsid = 0;

    int vle_bytes = 8;

    int max_buffer_size = (64 * 1024);

    int max_buffer_count = 32;

    private YaksImpl yaks; // yaks-api.ml
    private static YaksRuntimeImpl yaks_rt; // yaks-socket-driver.ml

    private YaksRuntimeImpl() {
    }

    public static synchronized YaksRuntimeImpl getInstance() {
        if (yaks_rt == null) {
            yaks_rt = new YaksRuntimeImpl();
        }
        return yaks_rt;
    }

    @Override
    public Yaks create(Properties properties) {
        YaksImpl yaks = new YaksImpl();
        int port;
        String host = (String) properties.get("host");
        int p = (int) properties.get("port");
        if (p == 0) {
            port = Yaks.DEFAUL_PORT;
        } else {
            port = p;
        }
        Selector selector;
        try {
            selector = Selector.open();

            InetSocketAddress addr;
            addr = new InetSocketAddress(InetAddress.getByName(host), port);

            this.socket = SocketChannel.open(addr);
            this.socket.setOption(StandardSocketOptions.TCP_NODELAY, true);
            this.socket.configureBlocking(false);
            this.socket.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            this.workingMap = new HashMap<Integer, Message>();
            this.listenersMap = new HashMap<String, Message>();
            this.evalsMap = new HashMap<Path, Message>();
            this.bytePool = new ByteBufferPoolImpl();

            yaks.setYaks_rt(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return yaks;
    }

    public ByteBuffer write(SocketChannel socket, Message msg) {

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

            if (socket.isConnected()) {
                socket.write((ByteBuffer) buf_vle.flip());
                socket.write(buf_msg);
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
        Map<String, String> propertiesList = new HashMap<String, String>();

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

    public boolean receiver_loop(Yaks yaks) {
        boolean isReplyOk = false;
        try {

            int vle = 0;

            // SocketChannel socket = yaks.getChannel();
            // while (vle == 0){
            while (true) {

                vle = VLEEncoder.read_vle(socket);
                Thread.sleep(YaksImpl.TIMEOUT);

                if (vle > 0) {
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msg = read(buffer);
                    if (workingMap.containsKey(msg.getCorrelationID())) {
                        workingMap.remove(msg).getCorrelationID();
                        switch (msg.getMessageCode()) {
                        case NOTIFY:
                            System.out.println("Received NOTIFY message");
                            isReplyOk = true;
                            break;
                        case VALUES:
                            System.out.println("Received VALUES message");
                            isReplyOk = true;
                            break;
                        case EVAL:
                            System.out.println("Received EVAL message");
                            isReplyOk = true;
                            break;
                        case OK:
                            System.out.println("Received OK message");
                            isReplyOk = true;
                            break;
                        default:
                        }
                    } else {
                        System.out.println("Received message with unknown correlation id :" + msg.getCorrelationID());
                    }
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isReplyOk;
    }

    @Override
    public void run() {
        receiver_loop(yaks);
    }

    @Override
    public void destroy(is.yaks.Yaks yaks) {
        // TODO Auto-generated method stub

    }

    @Override
    public void process_login(Properties properties, is.yaks.Yaks yaks) {
        int vle = 0;
        boolean is_login_ok = false;

        Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);
        // write msg
        yaks_rt.write(socket, loginM);
        // ==>
        while (vle == 0) {
            vle = VLEEncoder.read_vle(socket);
        }
        if (vle > 0) {
            // read response msg
            ByteBuffer buffer = ByteBuffer.allocate(vle);
            Message msgReply = yaks_rt.read(buffer);
            if (msgReply.getCorrelationID() == loginM.getCorrelationID()) {
                if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                    is_login_ok = true;
                }
            }
        }
    }

    @Override
    public Workspace process_workspace(Path path, Yaks yaks) {
        WorkspaceImpl ws = new WorkspaceImpl();
        int vle = 0;
        try {
            if (path != null) {
                int wsid = 0;

                Message workspaceM = new MessageFactory().getMessage(MessageCode.WORKSPACE, null);
                workspaceM.setPath(path);
                if (socket.isConnected()) {
                    // post msg
                    yaks_rt.write(socket, workspaceM);
                    // ==>
                    while (vle == 0) {
                        vle = VLEEncoder.read_vle(socket);
                    }
                    if (vle > 0) {
                        // read response msg
                        ByteBuffer buffer = ByteBuffer.allocate(vle);
                        socket.read(buffer);
                        Message msgReply = yaks_rt.read(buffer);

                        // check_if_corr_id is the same
                        if (msgReply.getCorrelationID() == workspaceM.getCorrelationID()) {
                            if (((Message) msgReply).getMessageCode().equals(MessageCode.OK)) {
                                // find_property wsid
                                Map<String, String> list = ((Message) msgReply).getPropertiesList();
                                if (!list.isEmpty()) {
                                    wsid = Integer.parseInt(list.get("wsid"));
                                }
                                ws.setWsid(wsid);
                                ws.setPath(path);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ws;
    }

    @Override
    public Map<Path, Value> process_get(Properties properties, YSelector selector, Yaks yaks, int quorum) {
        int vle = 0;
        Map<Path, Value> kvs = null;
        try {
            Message getM = new MessageFactory().getMessage(MessageCode.GET, null);
            getM.add_property(Message.WSID, String.valueOf(wsid));
            getM.add_selector(selector);
            if (yaks != null) {
                yaks_rt.write(socket, getM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    // if(msgReply.getCorrelationID() == getM.getCorrelationID()) {
                    if (msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) {
                        if (!msgReply.getValuesList().isEmpty()) {
                            kvs = msgReply.getValuesList();
                        }
                    }
                    // }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kvs;
    }

    @Override
    public boolean process_put(Properties properties, Path path, Value val, Yaks yaks, int quorum) {
        int vle = 0;
        boolean is_put_ok = false;

        try {
            Message putM = new MessageFactory().getMessage(MessageCode.PUT, null);
            putM.add_property(Message.WSID, String.valueOf(wsid));
            putM.add_workspace(path, val);
            if (socket != null) {
                yaks_rt.write(socket, putM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    // if (msgReply.getCorrelationID() == putM.getCorrelationID()) {
                    if ((msgReply) != null && msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_put_ok = true;
                    }
                    // }
                }
            } else {
                System.out.println("ERROR: socket is null!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return is_put_ok;
    }

    @Override
    public boolean process_update(Properties properties, Path p, Value v, is.yaks.Yaks yaks, int quorum) {
        int vle = 0;
        boolean is_update_ok = false;

        try {
            Message updateM = new MessageFactory().getMessage(MessageCode.UPDATE, null);
            updateM.add_property(Message.WSID, String.valueOf(wsid));
            updateM.add_workspace(p, v);
            if (socket != null) {
                yaks_rt.write(socket, updateM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    // if (msgReply.getCorrelationID() == putM.getCorrelationID()) {
                    if ((msgReply) != null && msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_update_ok = true;
                    }
                    // }
                }
            } else {
                System.out.println("ERROR: socket is null!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return is_update_ok;

    }

    @Override
    public boolean process_remove(Properties properties, Path path, Yaks yaks, int quorum) {
        int vle = 0;
        boolean is_remove_ok = false;
        try {
            Message deleteM = new MessageFactory().getMessage(MessageCode.DELETE, null);
            deleteM.add_property(Message.WSID, String.valueOf(wsid));
            deleteM.setPath(path);
            if (socket != null) {
                yaks_rt.write(socket, deleteM);

                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);

                    if (msgReply.getCorrelationID() == deleteM.getCorrelationID()) {
                        if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                            is_remove_ok = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is_remove_ok;
    }

    @Override
    public String process_subscribe(Properties properties, YSelector selector, Yaks yaks, Listener listener) {
        int vle = 0;
        String subid = "";
        try {
            Message subscribeM = new MessageFactory().getMessage(MessageCode.SUB, null);
            subscribeM.add_property(Message.WSID, String.valueOf(wsid));
            subscribeM.setPath(Path.ofString(selector.toString()));
            if (socket != null) {
                yaks_rt.write(socket, subscribeM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    if (msgReply.getCorrelationID() == subscribeM.getCorrelationID()) {
                        if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                            subid = (String) msgReply.getPropertiesList().get(Message.SUBID);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return subid;
    }

    @Override
    public boolean process_unsubscribe(String subid, Yaks yaks) {
        int vle = 0;
        boolean is_unsub_ok = false;
        try {
            Message unsubM = new MessageFactory().getMessage(MessageCode.UNSUB, null);
            unsubM.add_property(Message.WSID, String.valueOf(wsid));
            unsubM.setPath(Path.ofString(subid.toString()));
            if (socket != null) {
                yaks_rt.write(socket, unsubM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    if (msgReply.getCorrelationID() == unsubM.getCorrelationID()) {
                        if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                            is_unsub_ok = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is_unsub_ok;
    }

    @Override
    public void process_register_eval(Properties properties, Path path, Yaks yaks, Path workpath) {
        int vle = 0;
        boolean is_reg_eval_ok = false;
        try {

            Message regEvalM = new MessageFactory().getMessage(MessageCode.REG_EVAL, null);
            regEvalM.add_property(Message.WSID, String.valueOf(wsid));
            regEvalM.setPath(Path.ofString(path.toString()));
            if (socket != null) {
                yaks_rt.write(socket, regEvalM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    if (msgReply.getCorrelationID() == regEvalM.getCorrelationID()) {
                        if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                            is_reg_eval_ok = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process_unregister_eval(Properties properties, Path path, Yaks yaks, Path workpath) {
        int vle = 0;
        boolean is_unreg_eval_ok = false;
        try {
            Message unreg_evalM = new MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);
            unreg_evalM.add_property(Message.WSID, String.valueOf(wsid));
            unreg_evalM.setPath(Path.ofString(path.toString()));
            if (socket != null) {
                yaks_rt.write(socket, unreg_evalM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msgReply = yaks_rt.read(buffer);
                    if (msgReply.getCorrelationID() == unreg_evalM.getCorrelationID()) {
                        if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                            is_unreg_eval_ok = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Path, Value> process_eval(Properties properties, YSelector selector, Yaks yaks, int multiplicity) {
        int vle = 0;
        Message msgReply = null;
        String values = "";
        try {
            Message evalM = new MessageFactory().getMessage(MessageCode.EVAL, null);
            evalM.add_property(Message.WSID, String.valueOf(wsid));
            evalM.setSelector(selector);
            if (socket != null) {
                yaks_rt.write(socket, evalM);
                // ==>
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(socket);

                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    msgReply = yaks_rt.read(buffer);
                    if (msgReply.getCorrelationID() == evalM.getCorrelationID()) {
                        if (msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) {
                            if (!msgReply.getValuesList().isEmpty()) {

                                for (Map.Entry<Path, Value> pair : msgReply.getValuesList().entrySet()) {
                                    values += "(" + pair.getKey().toString() + ", "
                                            + pair.getValue().getValue().toString() + ")";
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msgReply.getValuesList();
    }

    @Override
    public void process_logout(is.yaks.Yaks yaks) {

    }

    @Override
    public List<Message> process() {
        return null;
    }

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

    public HashMap<String, Message> getListenersMap() {
        return listenersMap;
    }

    public void setListenersMap(HashMap<String, Message> listenersMap) {
        this.listenersMap = listenersMap;
    }

    public HashMap<Path, Message> getEvalsMap() {
        return evalsMap;
    }

    public void setEvalsMap(HashMap<Path, Message> evalsMap) {
        this.evalsMap = evalsMap;
    }

    public ByteBufferPoolImpl getBytePool() {
        return bytePool;
    }

    public void setBytePool(ByteBufferPoolImpl bytePool) {
        this.bytePool = bytePool;
    }

}
