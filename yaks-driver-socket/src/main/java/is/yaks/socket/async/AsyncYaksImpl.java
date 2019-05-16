package is.yaks.socket.async;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import is.yaks.Path;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public class AsyncYaksImpl implements AsyncYaks {

    private AsyncYaksRuntimeImpl async_yaks_rt;

    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public static final long TIMEOUT = 1l; // i.e 5l = 5ms, 1000l = i sec

    private AsyncAdminImpl async_admin;
    private AsyncWorkspace async_workspace;
    private static AsyncYaksImpl async_yaks;

    public AsyncYaksImpl() {
        async_yaks_rt = AsyncYaksRuntimeImpl.getInstance();
    }

    public AsyncYaksImpl(AsyncYaksImpl yaks) {
        async_yaks_rt = AsyncYaksRuntimeImpl.getInstance();
    }

    public static synchronized AsyncYaks getInstance() {
        if (async_yaks == null) {
            async_yaks = new AsyncYaksImpl();
        }
        return async_yaks;
    }

    @Override
    public AsyncYaks login(Properties properties) {
        
    	async_yaks = (AsyncYaksImpl) async_yaks_rt.create(properties);

    	System.out.println("[async_yaks_impl] Init receivers_loop() in yaks_runtime ... ");
    	executor.execute(async_yaks_rt);
    	
        async_yaks_rt.process_login(properties);
        
//      Runnable worker = new AsyncYaksRuntimeImpl("");
        
//      Thread thr1 = new Thread(async_yaks_rt, "Init async_yaks_rt");
//      thr1.start();

        return async_yaks;
    }

    @Override
    public AsyncAdmin admin() {
        async_admin = AsyncAdminImpl.getInstance();
        async_workspace = workspace(Path.ofString("/" + AsyncAdmin.PREFIX + "/" + AsyncAdmin.MY_YAKS));
        async_admin.setWorkspace(async_workspace);
        return async_admin;
    }

    /**
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     */
    @Override
    public AsyncWorkspace workspace(Path path) {
        return async_yaks_rt.process_workspace(path);
    }

    @Override
    public void close() {
        try {
            async_yaks_rt.getSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logout() {

    }

    public AsyncYaksRuntimeImpl getAsync_yaks_rt() {
        return async_yaks_rt;
    }

    public void setAsync_yaks_rt(AsyncYaksRuntimeImpl async_y_rt) {
        async_yaks_rt = async_y_rt;
    }

}
