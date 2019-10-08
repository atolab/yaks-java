import is.yaks.*;

public class YGet {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String selector = "/demo/example/**";
        if (args.length > 1) {
            selector = args[1];
        }

        try {
            Selector s = new Selector(selector);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Get from "+s);
            for (PathValue pv : w.get(s)) {
                System.out.println("  "+pv.getPath()+" : "+pv.getValue());
            }

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
