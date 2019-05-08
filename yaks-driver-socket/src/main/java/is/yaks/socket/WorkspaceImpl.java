package is.yaks.socket;

import java.util.Map;
import java.util.Properties;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.YSelector;
import is.yaks.Yaks;

public class WorkspaceImpl implements Workspace {

    public Path path = null;
    public int wsid = 0;

    private static WorkspaceImpl instance;
    Yaks yaks = YaksImpl.getInstance();
    // SocketChannel sock = yaks.getChannel();
    YaksRuntimeImpl yaks_rt = YaksRuntimeImpl.getInstance();
    Properties properties = new Properties();

    public WorkspaceImpl() {

    }

    public static synchronized Workspace getInstance() {
        if (instance == null) {
            instance = new WorkspaceImpl();
        }
        return instance;
    }

    @Override
    public boolean put(Path p, Value v, int quorum) {
        return yaks_rt.process_put(properties, p, v, yaks, quorum);
    }

    @Override
    public boolean update(Path p, Value v, int quorum) {
        return yaks_rt.process_update(properties, p, v, yaks, quorum);
    }

    @Override
    public Map<Path, Value> get(YSelector selector, int quorum) {
        return yaks_rt.process_get(properties, selector, yaks, quorum);
    }

    @Override
    public boolean remove(Path path, int quorum) {
        return yaks_rt.process_remove(properties, path, yaks, quorum);
    }

    @Override
    public String subscribe(YSelector selector, Listener listener) {
        return yaks_rt.process_subscribe(properties, selector, yaks, listener);
    }

    @Override
    public boolean unsubscribe(String subid) {
        return yaks_rt.process_unsubscribe(subid, yaks);
    }

    @Override
    public void register_eval(Path path, Path workpath, Listener evcb) {
        yaks_rt.process_register_eval(properties, path, yaks, workpath);
    }

    @Override
    public void unregister_eval(Path path) {

    }

    @Override
    public Map<Path, Value> eval(YSelector selector, int multiplicity) {
        return yaks_rt.process_eval(properties, selector, yaks, multiplicity);
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
