import is.yaks.*;

public class YPut {

    public static void main(String[] args) {
        String locator = null;
        if (args.length > 0) {
            locator = args[0];
        }

        // If not specified as 2nd argument, use a relative path (to the workspace below): "yaks-java-put"
        String path = "yaks-java-put";
        if (args.length > 1) {
            path = args[1];
        }

        String value = "Put from Yaks Java!";
        if (args.length > 2) {
            value = args[2];
        }

        try {
            Path p = new Path(path);
            Value v = new StringValue(value);

            System.out.println("Login to Yaks (locator="+locator+")...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/demo/example'");
            Workspace w = y.workspace(new Path("/demo/example"));

            System.out.println("Put on "+p+" : "+v);
            w.put(p, v);

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}