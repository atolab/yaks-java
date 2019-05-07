package is.yaks.socket.lib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.async.Yaks;
import is.yaks.socket.async.State;
import is.yaks.socket.async.YaksImpl;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class YaksRuntime implements Runnable {

    private YaksRuntime yaks_rt;

    public static int DEFAULT_TIMEOUT = 5;

    private HashMap<Integer, Message> workingMap = new HashMap<Integer, Message>();
    private HashMap<String, Message> listenersMap = new HashMap<String, Message>();
    private HashMap<Path, Message> evalsMap = new HashMap<Path, Message>();

    private Map<String, String> propertiesList;

    int vle_bytes = 8;

    int max_buffer_size = (64 * 1024);

    int max_buffer_count = 32;

    private static YaksRuntime instance;

    Runtime rt = null;

    private YaksRuntime() {
    }

    public static synchronized YaksRuntime getInstance() {
        if (instance == null) {
            instance = new YaksRuntime();
        }
        return instance;
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

    public boolean post_message(SocketChannel sock, Message msg) {

        return true;
    }

    public boolean read_message(YaksRuntime yaks_rt) {

        return true;
    }

    public CompletableFuture<Message> read(State driver) {

        return null;
    }

    // it creates the state called "driver" and return it
    public State create(Properties properties) {

        State driver = new State();

        int port;
        String h = (String) properties.get("host");
        String p = (String) properties.get("port");
        if (p.equals("")) {
            port = Yaks.DEFAUL_PORT;
        } else {
            port = Integer.parseInt(p);
        }
        try {

            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(h), port);
            Selector selector = Selector.open();
            SocketChannel sock = SocketChannel.open(addr);
            sock.setOption(StandardSocketOptions.TCP_NODELAY, true);
            sock.configureBlocking(false);
            sock.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            SelectionKey key = null;

            driver = driver.create(sock);

            // loop(driver);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return driver;
    }

    public boolean receiver_loop(Yaks yaks) {
        boolean isReplyOk = false;
        try {
            // CompletableFuture<Message> readFuture = read(yaks);
            // Message msgRead = readFuture.get();

            int vle = 0;

            SocketChannel sock = yaks.getChannel();

            while (vle == 0) {
                vle = VLEEncoder.read_vle(sock);
                Thread.sleep(YaksImpl.TIMEOUT);
            }
            if (vle > 0) {
                ByteBuffer buffer = ByteBuffer.allocate(vle);
                sock.read(buffer);
                Message msg = read(buffer);
                if (workingMap.containsKey(msg.getCorrelationID())) {
                    workingMap.remove(msg).getCorrelationID();
                    switch (msg.getMessageCode()) {
                    case NOTIFY:
                        isReplyOk = true;
                        break;
                    case VALUES:
                        isReplyOk = true;
                        break;
                    case EVAL:
                        isReplyOk = true;
                        break;
                    case OK:
                        isReplyOk = true;
                        break;
                    default:
                    }
                } else {
                    System.out.println("Received message with unknown correlation id :" + msg.getCorrelationID());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isReplyOk;
        /**
         * 1.
         */

        // ==> here it goes

        /*
         * let open Apero in let open Apero.Infix in MVar.read driver >>= fun self -> let%lwt len = Net.read_vle
         * self.sock >>= Vle.to_int %> Lwt.return in let%lwt _ = Logs_lwt.debug (fun m -> m "Message lenght : %d" len)
         * in let buf = Abuf.create len in let%lwt n = Net.read_all self.sock buf len in let () = check_socket self.sock
         * in let%lwt _ = Logs_lwt.debug (fun m -> m "Read %d bytes out of the socket" n) in (try decode_message buf
         * with e -> Logs.err (fun m -> m "Failed in parsing message %s" (Printexc.to_string e)) ; raise e) |> fun msg
         * -> match (msg.header.mid, msg.body) with | (NOTIFY, YNotification (subid, data)) -> MVar.read driver >>= fun
         * self -> (match ListenersMap.find_opt subid self.subscribers with | Some cb -> (* Run listener's callback in
         * future (catching exceptions) *) let _ = Lwt.try_bind (fun () -> let%lwt _ = Logs_lwt.debug (fun m -> m
         * "Notify received. Call listener for subscription %s" subid) in cb data) (fun () -> Lwt.return_unit) (fun ex
         * -> let%lwt _ = Logs_lwt.warn (fun m -> m
         * "Listener's callback of subscription %s raised an exception: %s\n %s" subid (Printexc.to_string ex)
         * (Printexc.get_backtrace ())) in Lwt.return_unit) in (* Return unit immediatly to release socket reading
         * thread *) Lwt.return_unit | None -> let%lwt _ = Logs_lwt.debug (fun m -> m
         * "Received notification with unknown subscriberid %s" subid) in Lwt.return_unit)
         * 
         * | (EVAL, YSelector s) -> (* Process eval in future (catching exceptions) *) let _ = Lwt.try_bind (fun () ->
         * process_eval s driver >>= fun results -> make_values msg.header.corr_id results >>= fun rmsg ->
         * send_to_socket rmsg self.buffer_pool self.sock) (fun () -> Lwt.return_unit) (fun ex -> let%lwt _ =
         * Logs_lwt.warn (fun m -> m "Eval's callback raised an exception: %s\n %s" (Printexc.to_string ex)
         * (Printexc.get_backtrace ())) in make_error msg.header.corr_id INTERNAL_SERVER_ERROR >>= fun rmsg ->
         * send_to_socket rmsg self.buffer_pool self.sock) in (* Return unit immediatly to release socket reading thread
         * *) Lwt.return_unit
         * 
         * | (_, _) -> MVar.guarded driver @@ (fun self -> (match WorkingMap.find_opt msg.header.corr_id
         * self.working_set with | Some (resolver, msg_list) -> let msg_list = List.append msg_list [msg] in if
         * (Yaks_fe_sock_types.has_incomplete_flag msg.header.flags) then MVar.return () {self with working_set =
         * WorkingMap.add msg.header.corr_id (resolver, msg_list) self.working_set} else let _ = Lwt.wakeup_later
         * resolver msg_list in MVar.return () {self with working_set = WorkingMap.remove msg.header.corr_id
         * self.working_set} | None -> let%lwt _ = Logs_lwt.warn (fun m -> m
         * "Received message with unknown correlation id %Ld" msg.header.corr_id) in MVar.return () self) )
         */
    }

    public void add_listener() {

    }

    public void remove_listener() {

    }

    public void add_eval_callback() {

    }

    public void remove_eval_callback() {

    }

    public void notify_listeners() {

    }

    public void execute_eval() {

    }

    public void handle_reply() {

    }

    public void handle_unexpected_message() {

    }

    public void close() {

    }

    @Override
    public void run() {
        String[] args = { "http://localhost:7887" };
        Yaks yaks = Yaks.getInstance("is.yaks.socket.YaksImpl", YaksRuntime.class.getClassLoader(), args);
        receiver_loop(yaks);
    }

}
