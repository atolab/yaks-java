package is.yaks.socket;

import java.util.Properties;

import is.yaks.Admin;
import is.yaks.Path;
import is.yaks.Workspace;
import is.yaks.Yaks;

public class YaksImpl implements Yaks {

    private AdminImpl admin;
    private Workspace workspace;
    private YaksRuntimeImpl yaks_rt;

    private static Yaks yaks;

    public static final long TIMEOUT = 1l;

    public YaksImpl() {
        yaks_rt = YaksRuntimeImpl.getInstance();
    }

    public static synchronized Yaks getInstance() {

        if (yaks == null) {
            yaks = new YaksImpl();
        }
        return yaks;
    }

    @Override
    public Yaks login(Properties properties) {

        yaks = yaks_rt.create(properties);

        yaks_rt.process_login(properties);

        return yaks;
    }

    @Override
    public Admin admin() {

        admin = AdminImpl.getInstance();

        workspace = workspace(Path.ofString("/" + Admin.PREFIX + "/" + Admin.MY_YAKS));

        admin.setWorkspace(workspace);

        return admin;
    }

    /**
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     */
    @Override
    public Workspace workspace(Path path) {
        return yaks_rt.process_workspace(path, yaks);
    }

    @Override
    public void close() {
        yaks_rt.destroy(yaks);

    }

    @Override
    public void logout() {
        yaks_rt.process_logout(yaks);
    }

    public AdminImpl getAdmin() {
        return admin;
    }

    public void setAdmin(AdminImpl admin) {
        this.admin = admin;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public YaksRuntimeImpl getYaks_rt() {
        return yaks_rt;
    }

    public void setYaks_rt(YaksRuntimeImpl yaks_rt) {
        this.yaks_rt = yaks_rt;
    }
}
