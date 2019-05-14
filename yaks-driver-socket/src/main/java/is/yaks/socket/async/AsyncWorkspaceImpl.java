package is.yaks.socket.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncWorkspace;

public class AsyncWorkspaceImpl implements AsyncWorkspace {

    public Path path = null;
    public int wsid = 0;

    private List<Observer> observers = new ArrayList<Observer>();
    // private List<Observer> eval_observers = new ArrayList<Observer>();

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
        notifyAllObserversPut(path, val);
        return async_yaks_rt.process_put(path, val, quorum);
    }

    @Override
    public boolean update(Path path, Value val, int quorum) {
        notifyAllObserversUpdate(path, val);
        return async_yaks_rt.process_update(path, val, quorum);
    }

    @Override
    public Map<Path, Value> get(YSelector selector, int quorum) {
        return async_yaks_rt.process_get(selector, quorum);
    }

    @Override
    public boolean remove(Path path, int quorum) {
        notifyAllObserversRemove(path, null);
        return async_yaks_rt.process_remove(path, quorum);
    }

    @Override
    public String subscribe(YSelector selector, Observer observer) {
        attach(observer);
        return async_yaks_rt.process_subscribe(selector, observer);
    }

    @Override
    public boolean unsubscribe(String subid) {
        return async_yaks_rt.process_unsubscribe(subid);
    }

    @Override
    public boolean register_eval(Path path, Observer eval_obs) {
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

    public void attach(Observer observer) {
        observers.add(observer);
    }

    public void notifyAllObserversPut(Path p, Value v) {
        if (!observers.isEmpty()) {
            for (Observer observer : observers) {
                observer.onPut(p, v);
            }
        }
    }

    public void notifyAllObserversUpdate(Path p, Value v) {
        if (!observers.isEmpty()) {
            for (Observer observer : observers) {
                observer.onUpdate(p, v);
            }
        }
    }

    public void notifyAllObserversRemove(Path p, Value v) {
        if (!observers.isEmpty()) {
            for (Observer observer : observers) {
                observer.onRemove(p, v);
            }
        }
    }

}
