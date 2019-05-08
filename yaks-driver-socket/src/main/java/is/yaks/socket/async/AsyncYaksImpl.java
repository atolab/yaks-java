package is.yaks.socket.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import is.yaks.Path;
import is.yaks.async.Admin;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.YaksRuntimeImpl;
import is.yaks.socket.async.AdminImpl;
import is.yaks.socket.types.Message;
import is.yaks.socket.types.MessageCode;
import is.yaks.socket.types.MessageFactory;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socket.utils.YaksConfiguration;

public class AsyncYaksImpl implements Yaks {

    private AsyncYaksRuntimeImpl async_yaks_rt;

    public static final long TIMEOUT = 1l; // i.e 5l = 5ms, 1000l = i sec

    private YaksConfiguration config = YaksConfiguration.getInstance();

    private static AsyncYaksImpl async_yaks;
    private static AdminImpl async_admin;
    private static Workspace async_workspace;

    private AsyncYaksImpl() {

    }

    public static synchronized Yaks getInstance() {
        if (async_yaks == null) {
            async_yaks = new AsyncYaksImpl();
        }
        return async_yaks;
    }

    private AsyncYaksImpl(String... args) {
        if (args.length == 0) {
            // logger.error("Usage: <yaksUrl>");
            System.exit(-1);
        }
        String yaksUrl = args[0];
        if (yaksUrl.isEmpty()) {
            System.exit(-1);
        }

        config.setYaksUrl(yaksUrl);
    }

    @Override
    public Yaks login(Properties properties) {
        CompletableFuture<Yaks> yaks_future = async_yaks_rt.create(properties);

        try {
            async_yaks_rt.process_login(properties, async_yaks);

            async_yaks = (AsyncYaksImpl) yaks_future.get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return async_yaks;
    }

    @SuppressWarnings("static-access")
    @Override
    public CompletableFuture<Admin> admin() {

        CompletableFuture<Workspace> workspace_future;

        CompletableFuture<Admin> admin_future = new CompletableFuture<Admin>();

        try {

            if (async_yaks.getAsync_yaks_rt().getSocket().isConnected()) {

                workspace_future = workspace(Path.ofString("/" + Admin.PREFIX + "/" + Admin.MY_YAKS));

                async_admin.setWorkspace(workspace_future.get());

                admin_future.complete((Admin) async_admin);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return admin_future;
    }

    /**
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     */
    @Override
    public CompletableFuture<Workspace> workspace(Path path) {
        return async_yaks_rt.process_workspace(path, async_yaks);
    }

    @Override
    public void close() {
        // try {
        //// socketChannel.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }

    @Override
    public void logout() {

    }

    public AsyncYaksRuntimeImpl getAsync_yaks_rt() {
        return async_yaks_rt;
    }

    public void setAsync_yaks_rt(AsyncYaksRuntimeImpl async_yaks_rt) {
        this.async_yaks_rt = async_yaks_rt;
    }

}
