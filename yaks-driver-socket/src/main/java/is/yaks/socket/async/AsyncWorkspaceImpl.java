package is.yaks.socket.async;

import java.util.Map;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public class AsyncWorkspaceImpl implements AsyncWorkspace {

    public Path path = null;
    public int wsid = 0;

    private static AsyncWorkspaceImpl instance;

    private AsyncYaksRuntimeImpl async_yaks_rt;

    public AsyncWorkspaceImpl() {
        async_yaks_rt = AsyncYaksRuntimeImpl.getInstance();
    }

    public static synchronized AsyncWorkspace getInstance() {
        if (instance == null) {
            instance = new AsyncWorkspaceImpl();
        }
        return instance;
    }

    @Override
    public boolean put(Path path, Value val, int quorum) {
        return async_yaks_rt.process_put(path, val, quorum);
    }

    @Override
    public boolean update(Path path, Value val, int quorum) {
        return async_yaks_rt.process_update(path, val, quorum);
    }

    @Override
    public Map<Path, Value> get(YSelector selector, int quorum) {
        return async_yaks_rt.process_get(selector, quorum);
    }

    @Override
    public boolean remove(Path path, int quorum) {
        return async_yaks_rt.process_remove(path, quorum);
    }

    @Override
    public String subscribe(YSelector selector, Listener listener) {
        return async_yaks_rt.process_subscribe(selector, listener);
    }

    @Override
    public boolean unsubscribe(String subid) {
        return async_yaks_rt.process_unsubscribe(subid);
    }

    @Override
    public boolean register_eval(Path path, Listener eval_obs) {
        return async_yaks_rt.process_register_eval(path, eval_obs);
    }

    @Override
    public boolean unregister_eval(Path path) {
        return async_yaks_rt.process_unregister_eval(path);
    }

    @Override
    public Map<Path, Value> eval(YSelector selector, int multiplicity) {
        return async_yaks_rt.process_eval(selector, multiplicity);
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
