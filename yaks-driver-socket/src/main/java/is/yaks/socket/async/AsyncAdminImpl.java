package is.yaks.socket.async;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;
import is.yaks.socket.types.Message;

public class AsyncAdminImpl implements AsyncAdmin {

    private static AsyncAdminImpl async_admin;

    private static AsyncWorkspace async_workspace;

    int quorum = 0;

    public AsyncAdminImpl() {

    }

    public static synchronized AsyncAdminImpl getInstance() {
        if (async_admin == null) {
            async_admin = new AsyncAdminImpl();
        }
        return async_admin;
    }

    @Override
    public boolean add_frontend(int feid, Properties properties, AsyncYaks async_yaks) {
        return false;
    }

    @Override
    public Value get_frontend(AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public List<Value> get_frontends(AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public boolean remove_frontend(String feid, AsyncYaks async_yaks) {
        return false;
    }

    @Override
    public boolean add_backend(String beid, Properties properties, AsyncYaks async_yaks) {
        return false;
    }

    @Override
    public Value get_backend(AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public List<Value> get_backends(String beid, AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public void remove_backend(String beid, AsyncYaks async_yaks) {

    }

    @Override
    public boolean add_storage(Properties properties, String stid, String beid) {

        if (beid.isEmpty()) {
            beid = "auto";
        }

        String p = "/" + AsyncAdmin.PREFIX + "/" + AsyncAdmin.MY_YAKS + "/backend/" + beid + "/storage/" + stid;

        Value v = new Value(properties.toString(), Encoding.PROPERTY);

        return async_workspace.put(Path.ofString(p), v, quorum);

    }

    @Override
    public Value get_storage(String stid, AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public List<Value> get_storages(String beid, AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public boolean remove_storage(String stid, AsyncYaks async_yaks) {
        boolean is_remove_storage_ok = false;
        CompletableFuture<Message> remove_storage_future = new CompletableFuture<Message>();
        String path = "/" + AsyncAdmin.PREFIX + "/" + AsyncAdmin.MY_YAKS + "/backend/*/storage/" + stid;
        Map<Path, Value> kvs = async_workspace.get(YSelector.ofString(path), quorum);
        if (kvs.size() > 0) {
            Iterator<Map.Entry<Path, Value>> it = kvs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Path, Value> pair = it.next();
                is_remove_storage_ok = async_workspace.remove(pair.getKey(), quorum);
            }
        }
        return is_remove_storage_ok;
    }

    @Override
    public List<Value> get_sessions(String feid, AsyncYaks async_yaks) {
        return null;
    }

    @Override
    public boolean close_session(String sid, AsyncYaks async_yaks) {
        return false;
    }

    @Override
    public List<Value> get_subscriptions(String sid, AsyncYaks async_yaks) {
        return null;
    }

    public void setWorkspace(AsyncWorkspace async_ws) {
        async_workspace = async_ws;
    }

}
