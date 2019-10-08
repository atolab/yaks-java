import is.yaks.*;

public class YRemove {

    public static void main(String[] args) {

        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        // If not specified as 2nd argument, use a relative path (to the workspace below): "yaks-java-put"
        String path = "yaks-java-put";
        if (args.length > 1) {
            path = args[1];
        }

        try {
            Path p = new Path(path);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/demo/example'");
            Workspace w = y.workspace(new Path("/demo/example"));

            System.out.println("Remove "+p);
            w.remove(p);

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}