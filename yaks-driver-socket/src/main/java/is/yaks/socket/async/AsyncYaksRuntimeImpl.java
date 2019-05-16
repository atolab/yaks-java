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

import is.yaks.Encoding;
import is.yaks.Observer;
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

    int vle_bytes = 10;

    int max_buffer_size = (64 * 1024);

    private SocketChannel socket;

    private HashMap<Long, CompletableFuture<Message>> workingFutureMap;
    private HashMap<String, CompletableFuture<Message>> evalsFutureMap;
    private HashMap<Path, Observer> evalsMap;
    private HashMap<String, Observer> listenersMap; // i am not using it yet

    private ByteBufferPoolImpl bytePool;

    private AsyncYaksImpl async_yaks; // yaks-api.ml
    private AsyncWorkspaceImpl async_workspace;

    private static AsyncYaksRuntimeImpl async_yaks_rt; // yaks-socket-driver.ml

    private Map<String, String> propertiesList;

    private Map<Path, Value> kvs;

    private String command;
    
    public AsyncYaksRuntimeImpl(String s) {
    	this.command = s;
    }

    public static synchronized AsyncYaksRuntimeImpl getInstance() {
        if (async_yaks_rt == null) {
            async_yaks_rt = new AsyncYaksRuntimeImpl("");
        }
        return async_yaks_rt;
    }

    @Override
    public void run() {
    	System.out.println(Thread.currentThread().getName()+" Start. Receiver_loop = "+command);
    	receiver_loop(async_yaks);
    	System.out.println(Thread.currentThread().getName()+" End.");
    }

    public boolean receiver_loop(AsyncYaks async_yaks) {
        boolean isReplyOk = false;
        try {
            long vle = 0;
            while (socket.isOpen() && socket.isConnected()) {
                vle = VLEEncoder.read_vle_from_socket(socket);
                Thread.sleep(AsyncYaksImpl.TIMEOUT);

                if (vle > 0) {
                    CompletableFuture<Message> msg_future = null;
                    ByteBuffer buffer = ByteBuffer.allocate((int) vle);
                    socket.read(buffer);
                    Message msg = read(buffer);

                    switch (msg.getMessageCode()) {
                    case NOTIFY:
                        break;
                    case VALUES:
                        if (workingFutureMap.containsKey(msg.getCorrelationID())) {
                            isReplyOk = true;
                            msg_future = workingFutureMap.get(msg.getCorrelationID());
                            msg_future.complete(msg);
                            workingFutureMap.remove(msg.getCorrelationID());
                        }
                        break;
                    case EVAL:
                        if (evalsFutureMap.containsKey(msg.getPath().toString())) {
                            msg_future = evalsFutureMap.get(msg.getPath().toString());
                            msg_future.complete(msg);
                            evalsFutureMap.remove(msg.getPath().toString());
                        }
                        break;
                    case OK:
                        if (workingFutureMap.containsKey(msg.getCorrelationID())) {
                            isReplyOk = true;
                            msg_future = workingFutureMap.get(msg.getCorrelationID());
                            msg_future.complete(msg);
                            workingFutureMap.remove(msg.getCorrelationID());
                        }
                        break;
                    default:
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

            this.workingFutureMap = new HashMap<Long, CompletableFuture<Message>>();
            this.evalsFutureMap = new HashMap<String, CompletableFuture<Message>>();
            this.listenersMap = new HashMap<String, Observer>();
            this.evalsMap = new HashMap<Path, Observer>();
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
            if (msg.getCorrelationID() > 0) {
                buf_msg.put(VLEEncoder.encode(msg.getCorrelationID()));
            }
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
            if (!msg.getSubid().equals("")) {
                buf_msg.put(VLEEncoder.encode(msg.getSubid().length()));
                buf_msg.put(msg.getSubid().getBytes());
            }
            if (!msg.get_values().isEmpty()) {
                buf_msg.put(Utils.valuesToByteBuffer(msg.get_values()));
            }
            // adding the msg length in vle encoding
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
        case 0xC2:
            msg = new MessageFactory().getMessage(MessageCode.EVAL, null);
            break;
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
        kvs = new HashMap<Path, Value>();
        propertiesList = new HashMap<String, String>();

        msg.setFlags((int) buffer.get()); // get the flags of the msg

        msg.setCorrelationId(VLEEncoder.read_vle_from_buffer(buffer)); // gets the correlation_id ( in vle)

        if (msgCode == 0xC2) {
            long length_value = VLEEncoder.read_vle_from_buffer(buffer);
            // System.out.println(" this is the length of the path it should be 17 : "+length_value);
            byte[] value_bytes = new byte[(int) length_value];
            buffer.get(value_bytes, 0, (int) length_value);
            try {
                strValue = new String(value_bytes, "UTF-8");
                // System.out.println(" this is the path : "+ strValue);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            msg.setPath(Path.ofString(strValue));
        }
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
                    long length_value = VLEEncoder.read_vle_from_buffer(buffer);
                    byte[] value_bytes = new byte[(int) length_value];
                    buffer.get(value_bytes, 0, (int) length_value);

                    try {
                        strKey = new String(key_bytes, "UTF-8");
                        strValue = new String(value_bytes, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    value = new Value();
                    value.setValue(strValue);
                    value.setEncoding(Encoding.getEncoding(val_encoding));
                    kvs.put(Path.ofString(strKey), value);
                    msg.add_values(kvs);
                }
            }
        }
        return msg;
    }

    @Override
    public boolean process_login(Properties properties) {
    	boolean is_login_ok = true;
    	Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);
    	CompletableFuture<Message> login_future = new CompletableFuture<Message>();
    	async_yaks_rt.write(socket, loginM);
    	workingFutureMap.put(loginM.getCorrelationID(), login_future);
    	try {
    		Message msg = login_future.get();
    		if (msg != null)
    			is_login_ok = true;
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	} catch (ExecutionException e) {
    		e.printStackTrace();
    	}
    	return is_login_ok;
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
                workingFutureMap.put(workspaceM.getCorrelationID(), ws_future);
            }
        }
        try {
        	Message msg = ws_future.get();
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
            workingFutureMap.put(putM.getCorrelationID(), put_future);
        }
        try {
            Message msg = put_future.get();
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
            workingFutureMap.put(getM.getCorrelationID(), get_future);
        }
        try {
            Message msg = get_future.get();
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
            workingFutureMap.put(updateM.getCorrelationID(), update_future);
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
            workingFutureMap.put(removeM.getCorrelationID(), remove_future);
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
    public String process_subscribe(YSelector yselector, Observer observer) {
        String subid = "";
        CompletableFuture<Message> sub_future = new CompletableFuture<Message>();
        Message subscribeM = new MessageFactory().getMessage(MessageCode.SUB, null);
        subscribeM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        subscribeM.setPath(Path.ofString(yselector.toString()));
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, subscribeM);
            workingFutureMap.put(subscribeM.getCorrelationID(), sub_future);
        }
        Message msg;
        try {
            msg = sub_future.get();
            Map<String, String> map = msg.getPropertiesList();
            if (!map.isEmpty()) {
                subid = msg.getPropertiesList().get(Message.SUBID);
                listenersMap.put(subid, observer);
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
        unsubM.setSubid(subid);
        if (socket.isConnected()) {
            async_yaks_rt.write(socket, unsubM);
            workingFutureMap.put(unsubM.getCorrelationID(), unsub_future);
        }
        try {
            Message msg = unsub_future.get();
            if (msg != null)
                listenersMap.remove(msg.getSubid());
            is_unsub_ok = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return is_unsub_ok;
    }

    @Override
    public boolean process_register_eval(Path path, Observer eval_obs) {
        boolean is_reg_eval_ok = false;
        CompletableFuture<Message> reg_eval_future = new CompletableFuture<Message>();
        Message regEvalM = new MessageFactory().getMessage(MessageCode.REG_EVAL, null);
        regEvalM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));
        regEvalM.setPath(Path.ofString(async_workspace.getPath().toString().concat(path.toString())));
        if (socket.isConnected()) {
            workingFutureMap.put(regEvalM.getCorrelationID(), reg_eval_future);
            async_yaks_rt.write(socket, regEvalM);
        }
        try {
            Message msg = reg_eval_future.get();
            if (msg != null) {
                is_reg_eval_ok = true;
                evalsMap.put(regEvalM.getPath(), eval_obs);
            }
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
            workingFutureMap.put(unregEvalM.getCorrelationID(), unreg_eval_future);
        }
        try {
            Message msg = unreg_eval_future.get();
            if (msg != null) {
                is_unreg_eval_ok = true;
                evalsMap.remove(unregEvalM.getPath());
                evalsFutureMap.remove(unregEvalM.getPath().toString());
            }
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
        Properties properties = new Properties();
        Map<Path, Value> kvs = new HashMap<Path, Value>();

        CompletableFuture<Message> msg_future_1 = new CompletableFuture<Message>();
        CompletableFuture<Message> msg_future_2 = new CompletableFuture<Message>();

        Message evalM = new MessageFactory().getMessage(MessageCode.EVAL, null);
        evalM.add_property(Message.WSID, String.valueOf(async_workspace.getWsid()));

        String sPath = "", sProp = "";
        if (yselector.toString().contains("?")) {
            sPath = async_workspace.getPath().toString()
                    .concat(yselector.toString().substring(0, yselector.toString().indexOf("?")));
            sProp = yselector.toString().substring(yselector.toString().indexOf("?") + 1);
            if (sProp.contains("(") && sProp.contains(")")) {
                sProp = sProp.substring(sProp.indexOf("(") + 1, sProp.indexOf(")"));
            }
            properties.put(sProp.substring(0, sProp.indexOf("=")), sProp.substring(sProp.indexOf("=") + 1));
        } else {
            sPath = async_workspace.getPath().toString().concat(yselector.toString());
        }
        evalM.setPath(new Path(sPath));
        async_yaks_rt.write(socket, evalM);
        workingFutureMap.put(evalM.getCorrelationID(), msg_future_1);
        evalsFutureMap.put(evalM.getPath().toString(), msg_future_2);

        try {
            Message msg_eval_return = msg_future_2.get();
            long corr_id = msg_eval_return.getCorrelationID();
            Observer eval_obs = evalsMap.get(msg_eval_return.getPath());
            if (eval_obs != null) {
                String eval_get = eval_obs.evalCallback(msg_eval_return.getPath(), properties);
                Message valuesM = new MessageFactory().getMessage(MessageCode.VALUES, null);
                kvs.put(msg_eval_return.getPath(), new Value(eval_get, Encoding.STRING));
                valuesM.add_values(kvs);
                valuesM.setCorrelationId(corr_id);
                async_yaks_rt.write(socket, valuesM);

                Message msg_values_return = msg_future_1.get();
                kvs = msg_values_return.get_values();
                if (!kvs.isEmpty()) {
                    for (Map.Entry<Path, Value> pair : kvs.entrySet()) {
                        values += "(" + pair.getKey().toString() + ", " + pair.getValue().getValue().toString() + ")";
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return kvs;
    }

    @Override
    public boolean process_logout(AsyncYaks async_yaks) {
    	//TODO
    	return true;
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

    public HashMap<Long, CompletableFuture<Message>> getWorkingMap() {
        return workingFutureMap;
    }

    public void setWorkingMap(HashMap<Long, CompletableFuture<Message>> workingMap) {
        this.workingFutureMap = workingMap;
    }

    public static AsyncYaksRuntimeImpl getAsync_yaks_rt() {
        return async_yaks_rt;
    }

    public static void setAsync_yaks_rt(AsyncYaksRuntimeImpl async_yaks_rt) {
        AsyncYaksRuntimeImpl.async_yaks_rt = async_yaks_rt;
    }

}
