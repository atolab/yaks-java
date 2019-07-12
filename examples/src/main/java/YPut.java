import is.yaks.*;

import java.util.regex.*;

public class YPut {

    public static void main(String[] args) {

        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String path = "/demo/hello/alpha";
        if (args.length > 1) {
            path = args[1];
        }

        String value = "Hello World!";
        if (args.length > 2) {
            value = args[2];
        }

        try {
            Path p = new Path(path);
            Value v = new StringValue(value);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Put on "+p+" : "+v);
            w.put(p, v);

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}