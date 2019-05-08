package is.yaks.socket.async;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.YSelector;
import is.yaks.Value;
import is.yaks.async.Admin;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;

public class AdminImpl implements Admin {

    private static AdminImpl instance;

    private static Workspace workspace;

    int quorum = 0;

    private AdminImpl() {

    }

    public static synchronized AdminImpl getInstance() {
        if (instance == null) {
            instance = new AdminImpl();
        }
        return instance;
    }

    @Override
    public CompletableFuture<Boolean> add_frontend(int feid, Properties properties, Yaks yaks) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<Value> get_frontend(Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<List<Value>> get_frontends(Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove_frontend(String feid, Yaks yaks) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<Boolean> add_backend(String beid, Properties properties, Yaks yaks) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<Value> get_backend(Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<List<Value>> get_backends(String beid, Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<Void> remove_backend(String beid, Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> add_storage(String stid, Properties properties, String beid, Yaks yaks) {

        if (beid.isEmpty()) {
            beid = "auto";
        }

        String p = "/" + Admin.PREFIX + "/" + Admin.MY_YAKS + "/backend/" + beid + "/storage/" + stid;

        Value v = new Value(properties.toString(), Encoding.PROPERTY);

        return workspace.put(Path.ofString(p), v, quorum);

    }

    @Override
    public Value get_storage(String stid, Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<List<Value>> get_storages(String beid, Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> remove_storage(String stid, Yaks yaks) {
        CompletableFuture<Boolean> is_remove_ok = new CompletableFuture<Boolean>();

        String p = "/" + Admin.PREFIX + "/" + Admin.MY_YAKS + "/backend/*/storage/" + stid;

        CompletableFuture<Map<Path, Value>> kvs = workspace.get(YSelector.ofString(p), quorum);

        // if(kvs.size() > 0) {
        // Iterator<Map.Entry<Path, Value>> it = kvs.entrySet().iterator();
        // while (it.hasNext()) {
        // Map.Entry<Path, Value> pair = it.next();
        // is_remove_ok = workspace.remove(pair.getKey(), quorum);
        // }
        // }
        return is_remove_ok;
    }

    @Override
    public CompletableFuture<List<Value>> get_sessions(String feid, Yaks yaks) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> close_session(String sid, Yaks yaks) {
        return CompletableFuture.supplyAsync(() -> false);
    }

    @Override
    public CompletableFuture<List<Value>> get_subscriptions(String sid, Yaks yaks) {
        return null;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        AdminImpl.workspace = workspace;
    }

}
