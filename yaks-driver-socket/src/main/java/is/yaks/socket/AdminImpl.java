package is.yaks.socket;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import is.yaks.Admin;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;

public class AdminImpl implements Admin {

    private static AdminImpl instance;

    private static Workspace workspace;

    private AdminImpl() {

    }

    public static synchronized AdminImpl getInstance() {
        if (instance == null) {
            instance = new AdminImpl();
        }
        return instance;
    }

    @Override
    public boolean add_frontend(int feid, Properties properties, Yaks yaks) {
        return false;
    }

    @Override
    public Value get_frontend(Yaks yaks) {
        return null;
    }

    @Override
    public List<Value> get_frontends(Yaks yaks) {
        return null;
    }

    @Override
    public boolean remove_frontend(String feid, Yaks yaks) {
        return false;
    }

    @Override
    public boolean add_backend(String beid, Properties properties, Yaks yaks) {
        return false;
    }

    @Override
    public Value get_backend(Yaks yaks) {
        return null;
    }

    @Override
    public List<Value> get_backends(String beid, Yaks yaks) {
        return null;
    }

    @Override
    public void remove_backend(String beid, Yaks yaks) {

    }

    @Override
    public boolean add_storage(String stid, Properties properties, String beid, Yaks yaks) {

        boolean is_reply_ok = false;

        if (beid.isEmpty()) {
            beid = "auto";
        }
        String p = "/" + Admin.PREFIX + "/" + Admin.MY_YAKS + "/backend/" + beid + "/storage/" + stid;

        Value v = new Value(properties.toString(), Encoding.PROPERTY);

        int quorum = 1;

        is_reply_ok = workspace.put(Path.ofString(p), v, quorum);

        return is_reply_ok;
    }

    @Override
    public Value get_storage(String stid, Yaks yaks) {
        return null;
    }

    @Override
    public List<Value> get_storages(String beid, Yaks yaks) {
        return null;
    }

    @Override
    public boolean remove_storage(String stid, Yaks yaks) {
        boolean is_remove_ok = false;
        int quorum = 1;
        String p = "/" + Admin.PREFIX + "/" + Admin.MY_YAKS + "/backend/*/storage/" + stid;

        System.out.println("remove_storage: " + p);

        Map<Path, Value> kvs = workspace.get(Selector.ofString(p));

        if ((kvs != null) && (kvs.size() > 0)) {
            Iterator<Map.Entry<Path, Value>> it = kvs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Path, Value> pair = it.next();
                is_remove_ok = workspace.remove(pair.getKey(), quorum);
            }
        }
        return is_remove_ok;

    }

    @Override
    public List<Value> get_sessions(String feid, Yaks yaks) {
        return null;
    }

    @Override
    public boolean close_session(String sid, Yaks yaks) {
        return false;
    }

    @Override
    public List<Value> get_subscriptions(String sid, Yaks yaks) {
        return null;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        AdminImpl.workspace = workspace;
    }

}
