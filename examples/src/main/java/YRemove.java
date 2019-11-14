import is.yaks.*;

public class YRemove {

    public static void main(String[] args) {
        // If not specified as 1st argument, use a relative path (to the workspace below): "yaks-java-put"
        String path = "yaks-java-put";
        if (args.length > 0) {
            path = args[0];
        }

        String locator = null;
        if (args.length > 1) {
            locator = args[1];
        }

        try {
            Path p = new Path(path);

            System.out.println("Login to Yaks (locator="+locator+")...");
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