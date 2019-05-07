package is.yaks.socket.lib;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;

public interface YaksSocketDriver {

    public CompletableFuture<Void> create(String locator);

    public <T> CompletableFuture<Void> destroy(T t);

    public <T> CompletableFuture<Void> process_login(Properties properties, T t);

    public <T> CompletableFuture<Void> process_logout(T t);

    public <T> CompletableFuture<String> process_workspace(Path path, T t);

    public <T> CompletableFuture<Map<Path, Value>> process_get(Properties properties, Selector selector, T t,
            int quorum);

    public <T> CompletableFuture<Void> process_put(Properties properties, Path path, Value val, T t, int quorum);

    public <T> CompletableFuture<Void> process_update(Properties properties, Path path, Value val, T t, int quorum);

    public <T> CompletableFuture<Void> process_remove(Properties properties, T t, int quorum);

    public <T> CompletableFuture<String> process_subscribe(Properties properties, Selector selector, T t,
            Listener listener);

    public <T> CompletableFuture<Void> process_unsubscribe(String subid, T t);

    public <T> CompletableFuture<Void> process_register_eval(Properties properties, Path path, T t, Path workpath);

    public <T> CompletableFuture<Void> process_unregister_eval(Properties properties, Path path, T t, Path workpath);

    public <T> CompletableFuture<Map<Path, Value>> process_eval(Properties properties, Selector selector, T t,
            int multiplicity);

    public CompletableFuture<List<Message>> process();

}
