package is.yaks.socket.async;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.types.Message;

public interface AsyncYaksRuntime {

    public CompletableFuture<Yaks> create(Properties properties);

    public CompletableFuture<Void> destroy(Yaks yaks);

    public CompletableFuture<Void> process_login(Properties properties, Yaks yaks);

    public CompletableFuture<Void> process_logout(Yaks yaks);

    public CompletableFuture<Workspace> process_workspace(Path path, Yaks yaks);

    public CompletableFuture<Map<Path, Value>> process_get(Properties properties, YSelector yselector, Yaks yaks,
            int quorum);

    public CompletableFuture<Boolean> process_put(Properties properties, Path path, Value val, Yaks yaks, int quorum);

    public CompletableFuture<Boolean> process_update(Properties properties, Path path, Value val, Yaks yaks,
            int quorum);

    public CompletableFuture<Boolean> process_remove(Properties properties, Yaks yaks, int quorum);

    public CompletableFuture<String> process_subscribe(Properties properties, YSelector yselector, Yaks yaks,
            Listener listener);

    public CompletableFuture<Boolean> process_unsubscribe(String subid, Yaks yaks);

    public CompletableFuture<Void> process_register_eval(Properties properties, Path path, Yaks yaks, Path workpath);

    public CompletableFuture<Void> process_unregister_eval(Properties properties, Path path, Yaks yaks, Path workpath);

    public CompletableFuture<Map<Path, Value>> process_eval(Properties properties, YSelector yselector, Yaks yaks,
            int multiplicity);

    public CompletableFuture<Void> process();
}
