package is.yaks.socket.async;

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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.Yaks;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;
import is.yaks.socket.types.Message;
import is.yaks.socket.types.MessageCode;
import is.yaks.socket.types.MessageFactory;
import is.yaks.socket.types.MessageImpl;
import is.yaks.socket.utils.ByteBufferPoolImpl;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;

public class AsyncYaksRuntimeImpl implements AsyncYaksRuntime, Runnable {

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    int vle_bytes = 8;

    int max_buffer_size = (64 * 1024);

    private SocketChannel socket;
    private HashMap<Integer, CompletableFuture<Message>> workingMap;
    private HashMap<String, Future<Message>> listenersMap;
    private HashMap<Path, Future<Message>> evalsMap;
    private ByteBufferPoolImpl bytePool;

    private AsyncYaksImpl async_yaks; // yaks-api.ml
    private AsyncWorkspaceImpl async_workspace;

    private static AsyncYaksRuntimeImpl async_yaks_rt; // yaks-socket-driver.ml

    private Map<String, String> propertiesList;

    private AsyncYaksRuntimeImpl() {
    }

    public static synchronized AsyncYaksRuntimeImpl getInstance() {
        if (async_yaks_rt == null) {
            async_yaks_rt = new AsyncYaksRuntimeImpl();
        }
        return async_yaks_rt;
    }

    @Override
    public void run() {
        receiver_loop(async_yaks);
    }

    public boolean receiver_loop(AsyncYaks async_yaks) {
        boolean isReplyOk = false;
        try {

            int vle = 0;

            while (socket.isOpen()) {

                vle = VLEEncoder.read_vle(socket);
                Thread.sleep(AsyncYaksImpl.TIMEOUT);

                if (vle > 0) {
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    socket.read(buffer);
                    Message msg = read(buffer);
                    if (workingMap.containsKey(msg.getCorrelationID())) {

                        // get the CompletableFuture<Message>> and completed it.
                        CompletableFuture<Message> msg_future = workingMap.get(msg.getCorrelationID());

                        msg_future.complete(msg);

                        workingMap.remove(msg.getCorrelationID());
                        switch (msg.getMessageCode()) {
                        case NOTIFY:
                            System.out.println("[async_yaks_rt] Received NOTIFY message");
                            isReplyOk = true;
                            break;
                        case VALUES:
                            System.out.println("[async_yaks_rt] Received VALUES message");
                            isReplyOk = true;
                            break;
                        case EVAL:
                            System.out.println("[async_yaks_rt] Received EVAL message");
                            isReplyOk = true;
                            break;
                        case OK:
                            System.out.println("[async_yaks_rt] Received OK message");

                            // CompletableFuture<Message> msg_future = workingMap.get(msg.getCorrelationID());
                            // msg_future.complete(msg);

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
    public AsyncYaks create(Properties properties) {
        int port;
        async_yaks = new AsyncYaksImpl();

        String host = (String) properties.get("host");
        int p = Integer.parseInt((String) properties.get("port"));
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

            this.workingMap = new HashMap<Integer, CompletableFuture<Message>>();
            this.listenersMap = new HashMap<String, Future<Message>>();
            this.evalsMap = new HashMap<Path, Future<Message>>();
            this.bytePool = new ByteBufferPoolImpl();

            async_yaks.setAsync_yaks_rt(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return async_yaks;
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
    public void process_login(Properties properties) {
        Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);

        CompletableFuture<Message> login_future = new CompletableFuture<Message>();

        async_yaks_rt.write(socket, loginM);

        workingMap.put(loginM.getCorrelationID(), login_future);

        System.out.println("[async_yaks_rt] Init receivers_loop in yaks_runtime ... ");

        Thread thr1 = new Thread(async_yaks_rt, "Init async_yaks_rt");
        thr1.start();
    }

    @Override
    public AsyncWorkspace process_workspace(Path path) {
        int wsid = 0;
        CompletableFuture<Message> ws_future = new CompletableFuture<Message>();
        async_workspace = new AsyncWorkspaceImpl();
        if (path != null) {
            Message workspaceM = new MessageFactory().getMessage(MessageCode.WORKSPACE, null);
            workspaceM.setPath(path);
            if (socket.isConnected()) {
                async_yaks_rt.write(socket, workspaceM);
                workingMap.put(workspaceM.getCorrelationID(), ws_future);
            }
        }
        Message msg;
        try {
            msg = ws_future.get();
            Map<String, String> map = msg.getPropertiesList();
            if (!map.isEmpty()) {
                wsid = Integer.parseInt(map.get("wsid"));
            }
            async_workspace.setWsid(wsid);
            async_workspace.setPath(path);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return async_workspace;
    }

    @Override
    public boolean process_put(Path path, Value val, int quorum) {
        boolean is_put_ok = true;
        CompletableFuture<Message> put_future = new CompletableFuture<Message>();

        Message putM = new MessageFactory().getMessage(MessageCode.PUT, null);
        putM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        putM.add_workspace(path, val);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, putM);
            workingMap.put(putM.getCorrelationID(), put_future);
        }
        Message msg;
        try {
            msg = put_future.get();
            if (msg != null)
                is_put_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_put_ok;
    }

    @Override
    public Map<Path, Value> process_get(YSelector yselector, int quorum) {
        Map<Path, Value> kvs = new HashMap<Path, Value>();
        CompletableFuture<Message> get_future = new CompletableFuture<Message>();
        Message getM = new MessageFactory().getMessage(MessageCode.GET, null);
        getM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        getM.add_selector(yselector);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, getM);
            workingMap.put(getM.getCorrelationID(), get_future);
        }
        Message msg;
        try {
            msg = get_future.get();
            kvs = msg.getValuesList();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return kvs;
    }

    @Override
    public boolean process_update(Path path, Value val, int quorum) {
        boolean is_update_ok = true;
        CompletableFuture<Message> update_future = new CompletableFuture<Message>();
        Message updateM = new MessageFactory().getMessage(MessageCode.UPDATE, null);
        updateM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        updateM.add_workspace(path, val);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, updateM);
            workingMap.put(updateM.getCorrelationID(), update_future);
        }
        Message msg;
        try {
            msg = update_future.get();
            if (msg != null)
                is_update_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_update_ok;
    }

    @Override
    public boolean process_remove(Path path, int quorum) {
        boolean is_remove_ok = false;
        CompletableFuture<Message> remove_future = new CompletableFuture<Message>();
        Message removeM = new MessageFactory().getMessage(MessageCode.DELETE, null);
        removeM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        removeM.setPath(path);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, removeM);
            workingMap.put(removeM.getCorrelationID(), remove_future);
        }
        Message msg;
        try {
            msg = remove_future.get();
            if (msg != null)
                is_remove_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_remove_ok;
    }

    @Override
    public String process_subscribe(YSelector yselector, Listener listener) {
        String subid = "";
        CompletableFuture<Message> sub_future = new CompletableFuture<Message>();

        Message subscribeM = new MessageFactory().getMessage(MessageCode.SUB, null);
        subscribeM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        subscribeM.setPath(Path.ofString(yselector.toString()));
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, subscribeM);
            workingMap.put(subscribeM.getCorrelationID(), sub_future);
        }
        Message msg;
        try {
            msg = sub_future.get();
            Map<String, String> map = msg.getPropertiesList();
            if (!map.isEmpty()) {
                subid = msg.getPropertiesList().get(Message.SUBID);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return subid;
    }

    @Override
    public boolean process_unsubscribe(String subid) {
        boolean is_unsub_ok = false;
        CompletableFuture<Message> unsub_future = new CompletableFuture<Message>();
        Message unsubM = new MessageFactory().getMessage(MessageCode.UNSUB, null);
        unsubM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        unsubM.add_property(Message.SUBID, String.valueOf(subid.toString()));
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, unsubM);
            workingMap.put(unsubM.getCorrelationID(), unsub_future);
        }
        Message msg;
        try {
            msg = unsub_future.get();
            if (msg != null)
                is_unsub_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_unsub_ok;
    }

    @Override
    public boolean process_register_eval(Path path, Listener eval_obs) {
        boolean is_reg_eval_ok = false;
        CompletableFuture<Message> reg_eval_future = new CompletableFuture<Message>();

        Message regEvalM = new MessageFactory().getMessage(MessageCode.REG_EVAL, null);
        regEvalM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        regEvalM.setPath(Path.ofString(path.toString()));
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, regEvalM);
            workingMap.put(regEvalM.getCorrelationID(), reg_eval_future);
        }
        Message msg;
        try {
            msg = reg_eval_future.get();
            if (msg != null)
                is_reg_eval_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_reg_eval_ok;
    }

    @Override
    public boolean process_unregister_eval(Path path) {
        boolean is_unreg_eval_ok = false;
        CompletableFuture<Message> unreg_eval_future = new CompletableFuture<Message>();

        Message unregEvalM = new MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);
        unregEvalM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        unregEvalM.setPath(Path.ofString(path.toString()));
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, unregEvalM);
            workingMap.put(unregEvalM.getCorrelationID(), unreg_eval_future);
        }
        Message msg;
        try {
            msg = unreg_eval_future.get();
            if (msg != null)
                is_unreg_eval_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_unreg_eval_ok;
    }

    @Override
    public Map<Path, Value> process_eval(YSelector yselector, int multiplicity) {
        String values = "";
        Map<Path, Value> kvs = new HashMap<Path, Value>();
        CompletableFuture<Message> eval_future = new CompletableFuture<Message>();

        Message evalM = new MessageFactory().getMessage(MessageCode.EVAL, null);
        evalM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        evalM.setSelector(yselector);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, evalM);
            workingMap.put(evalM.getCorrelationID(), eval_future);
        }
        Message msg;
        try {
            msg = eval_future.get();
            kvs = msg.getValuesList();
            if (!kvs.isEmpty()) {
                for (Map.Entry<Path, Value> pair : kvs.entrySet()) {
                    values += "(" + pair.getKey().toString() + ", " + pair.getValue().getValue().toString() + ")";
                }
            }
            System.out.println("[eval]: values: " + values);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return kvs;
    }

    @Override
    public void process_logout(AsyncYaks async_yaks) {

    }

    @Override
    public void destroy(AsyncYaks async_yaks) {

    }

    @Override
    public void process() {

    }

    public SocketChannel getSocket() {
        return socket;
    }

    public void setSocket(SocketChannel socket) {
        this.socket = socket;
    }

    public HashMap<Integer, CompletableFuture<Message>> getWorkingMap() {
        return workingMap;
    }

    public void setWorkingMap(HashMap<Integer, CompletableFuture<Message>> workingMap) {
        this.workingMap = workingMap;
    }

    public static AsyncYaksRuntimeImpl getAsync_yaks_rt() {
        return async_yaks_rt;
    }

    public static void setAsync_yaks_rt(AsyncYaksRuntimeImpl async_yaks_rt) {
        AsyncYaksRuntimeImpl.async_yaks_rt = async_yaks_rt;
    }

}
