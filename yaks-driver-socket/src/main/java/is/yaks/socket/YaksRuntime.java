package is.yaks.socket;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.YSelector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.types.Message;

public interface YaksRuntime {

    public Yaks create(Properties properties);

    public void destroy(Yaks yaks);

    public void process_login(Properties properties);

    public void process_logout(Yaks yaks);

    public Workspace process_workspace(Path path, Yaks yaks);

    public Map<Path, Value> process_get(Properties properties, YSelector yselector, Yaks yaks, int quorum);

    public boolean process_put(Properties properties, Path path, Value val, Yaks yaks, int quorum);

    public boolean process_update(Properties properties, Path path, Value val, Yaks yaks, int quorum);

    public boolean process_remove(Properties properties, Path path, Yaks yaks, int quorum);

    public String process_subscribe(Properties properties, YSelector yselector, Yaks yaks, Listener listener);

    public boolean process_unsubscribe(String subid, Yaks yaks);

    public void process_register_eval(Properties properties, Path path, Yaks yaks, Path workpath);

    public void process_unregister_eval(Properties properties, Path path, Yaks yaks, Path workpath);

    public Map<Path, Value> process_eval(Properties properties, YSelector yselector, Yaks yaks, int multiplicity);

    public List<Message> process();
}
