package is.yaks.socket.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.lib.Message;
import is.yaks.socket.lib.MessageFactory;
import is.yaks.socket.lib.YaksRuntime;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class WorkspaceImpl implements Workspace {

    public Path path = null;
    public int wsid = 0;

    private static WorkspaceImpl instance;
    YaksRuntime rt = YaksRuntime.getInstance();
    static SocketChannel sock = null;

    public WorkspaceImpl() {
        Yaks yaks = YaksImpl.getInstance();
        sock = yaks.getChannel();
    }

    public static synchronized Workspace getInstance() {
        if (instance == null) {
            instance = new WorkspaceImpl();
        }
        return instance;
    }

    @Override
    public CompletableFuture<Boolean> put(Path p, Value v, int quorum) {

        CompletableFuture<Boolean> is_put_ok = new CompletableFuture<Boolean>();
        try {
            Message putM = new MessageFactory().getMessage(MessageCode.PUT, null);
            putM.add_property(Message.WSID, String.valueOf(wsid));
            putM.add_workspace(p, v);
            if (sock != null) {
                rt.write(sock, putM);
                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if ((msgReply != null) && (msgReply.getMessageCode().equals(MessageCode.OK))) {
                        is_put_ok.complete(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return is_put_ok;
    }

    @Override
    public CompletableFuture<Void> update() {
        return null;
    }

    @Override
    public CompletableFuture<Map<Path, Value>> get(Selector select, int quorum) {

        CompletableFuture<Map<Path, Value>> kvs = new CompletableFuture<Map<Path, Value>>();
        try {

            Message getM = new MessageFactory().getMessage(MessageCode.GET, null);

            getM.add_property(Message.WSID, String.valueOf(wsid));
            getM.add_selector(select);
            if (sock != null) {
                rt.write(sock, getM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if ((msgReply != null) && (msgReply.getMessageCode().equals(MessageCode.VALUES))) {
                        if (!((Message) msgReply).getValuesList().isEmpty()) {
                            kvs.complete(msgReply.getValuesList());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return kvs;
    }

    @Override
    public CompletableFuture<Boolean> remove(Path path, int quorum) {

        CompletableFuture<Boolean> is_remove_ok = new CompletableFuture<Boolean>();

        try {

            Message deleteM = new MessageFactory().getMessage(MessageCode.DELETE, null);

            deleteM.add_property(Message.WSID, String.valueOf(wsid));
            deleteM.setPath(path);
            if (sock != null) {
                rt.write(sock, deleteM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer2 = ByteBuffer.allocate(vle);
                    sock.read(buffer2);
                    Message msgReply = rt.read(buffer2);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_remove_ok.complete(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return is_remove_ok;
    }

    @Override
    public CompletableFuture<String> subscribe(Selector selector, Listener listener) {
        CompletableFuture<String> subid = new CompletableFuture<String>();
        try {

            Message subscribeM = new MessageFactory().getMessage(MessageCode.SUB, null);
            subscribeM.add_property(Message.WSID, String.valueOf(wsid));
            subscribeM.setPath(Path.ofString(selector.toString()));
            if (sock != null) {
                rt.write(sock, subscribeM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        subid.complete(msgReply.getPropertiesList().get(Message.SUBID));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return subid;
    }

    @Override
    public CompletableFuture<String> subscribe(Selector selector) {
        CompletableFuture<String> subid = new CompletableFuture<String>();
        try {

            Message subscribeM = new MessageFactory().getMessage(MessageCode.SUB, null);
            subscribeM.add_property(Message.WSID, String.valueOf(wsid));
            subscribeM.setPath(Path.ofString(selector.toString()));
            if (sock != null) {
                rt.write(sock, subscribeM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        subid.complete(msgReply.getPropertiesList().get(Message.SUBID));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return subid;
    }

    @Override
    public CompletableFuture<Boolean> unsubscribe(String subid) {
        CompletableFuture<Boolean> is_unsub_ok = new CompletableFuture<Boolean>();
        try {

            Message unsubM = new MessageFactory().getMessage(MessageCode.UNSUB, null);
            unsubM.add_property(Message.WSID, String.valueOf(wsid));
            unsubM.setPath(Path.ofString(subid.toString()));
            if (sock != null) {
                rt.write(sock, unsubM);
                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_unsub_ok.complete(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return is_unsub_ok;
    }

    @Override
    public CompletableFuture<Boolean> register_eval(Path path, Listener evcb) {
        CompletableFuture<Boolean> is_regeval_ok = new CompletableFuture<Boolean>();
        try {

            Message regEvalM = new MessageFactory().getMessage(MessageCode.REG_EVAL, null);
            regEvalM.add_property(Message.WSID, String.valueOf(wsid));
            regEvalM.setPath(Path.ofString(path.toString()));
            if (sock != null) {
                rt.write(sock, regEvalM);
                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_regeval_ok.complete(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return is_regeval_ok;
    }

    @Override
    public CompletableFuture<Boolean> unregister_eval(Path path) {

        CompletableFuture<Boolean> is_unregeval_ok = new CompletableFuture<Boolean>();
        try {

            Message unreg_evalM = new MessageFactory().getMessage(MessageCode.UNREG_EVAL, null);
            unreg_evalM.add_property(Message.WSID, String.valueOf(wsid));
            unreg_evalM.setPath(Path.ofString(path.toString()));
            if (sock != null) {
                rt.write(sock, unreg_evalM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply.getMessageCode().equals(MessageCode.OK)) {
                        is_unregeval_ok.complete(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return is_unregeval_ok;
    }

    @Override
    public CompletableFuture<String> eval(Selector selector) {
        CompletableFuture<String> values = null;
        try {

            Message evalM = new MessageFactory().getMessage(MessageCode.EVAL, null);
            evalM.add_property(Message.WSID, String.valueOf(wsid));
            evalM.setSelector(selector);
            if (sock != null) {
                rt.write(sock, evalM);

                // ==>
                int vle = 0;
                while (vle == 0) {
                    vle = VLEEncoder.read_vle(sock);
                    Thread.sleep(YaksImpl.TIMEOUT);
                }
                if (vle > 0) {
                    // read response msg
                    ByteBuffer buffer = ByteBuffer.allocate(vle);
                    sock.read(buffer);
                    Message msgReply = rt.read(buffer);

                    if (msgReply != null && msgReply.getMessageCode().equals(MessageCode.VALUES)) {
                        if (!msgReply.getValuesList().isEmpty()) {

                            for (Map.Entry<Path, Value> pair : ((Message) msgReply).getValuesList().entrySet()) {
                                values.complete("(" + pair.getKey().toString() + ", "
                                        + pair.getValue().getValue().toString() + ")");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return values;
    }

    public void setPath(Path p) {
        path = p;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public void setWsid(int id) {
        wsid = id;
    }

    @Override
    public int getWsid() {
        return wsid;
    }
}
