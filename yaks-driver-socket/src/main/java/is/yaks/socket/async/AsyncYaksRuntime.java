package is.yaks.socket.async;

import java.util.Map;
import java.util.Properties;

import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public interface AsyncYaksRuntime {

    public AsyncYaks create(Properties properties);

    public void destroy(AsyncYaks yaks);

    public boolean process_login(Properties properties);

    public boolean process_logout(AsyncYaks yaks);

    public AsyncWorkspace process_workspace(Path path);

    public Map<Path, Value> process_get(YSelector yselector, int quorum);

    public boolean process_put(Path path, Value val, int quorum);

    public boolean process_update(Path path, Value val, int quorum);

    public boolean process_remove(Path path, int quorum);

    public String process_subscribe(YSelector yselector, Observer obs);

    public boolean process_unsubscribe(String subid);

    public boolean process_register_eval(Path path, Observer eval_obs);

    public boolean process_unregister_eval(Path path);

    public Map<Path, Value> process_eval(YSelector yselector, int multiplicity);

    public void process();
}
