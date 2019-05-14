package is.yaks.socket.types;

import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.socket.async.AsyncWorkspaceImpl;

public class ObserverImpl implements Observer {
    private static ObserverImpl obs_instance;
    public AsyncWorkspaceImpl async_workspace;

    public ObserverImpl() {
    }

    public static synchronized Observer getInstance() {
        if (obs_instance == null) {
            obs_instance = new ObserverImpl();
        }
        return obs_instance;
    }

    public ObserverImpl(AsyncWorkspaceImpl a_ws) {
        this.async_workspace = a_ws;
        this.async_workspace.attach(this);
    }

    @Override
    public void onPut(Path key, Value value) {
        System.out.println(">>>> [APP] [OBS] received Put of Key: " + key + " Value: " + value);

    }

    @Override
    public void onUpdate(Path key, Value value) {
        System.out.println(">>>> [APP] [OBS] received Update of Key: " + key + " Value: " + value);

    }

    @Override
    public void onRemove(Path key, Value value) {
        System.out.println(">>>> [APP] [OBS] received Remove of Key: " + key + " Value: " + value);

    }
}