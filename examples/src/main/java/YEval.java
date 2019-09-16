import is.yaks.*;

import java.util.Properties;

public class YEval {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String p = "/demo/eval";
        if (args.length > 1) {
            p = args[1];
        }

        try {
            Path path = new Path(p);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Register eval "+path);
            w.registerEval(path, 
                new Eval() {
                    public Value callback(Path unused, Properties properties) {
                        String name = properties.getProperty("name", "World");
                        return new StringValue("Hello "+name+"!");
                    }
                }
            );

            Thread.sleep(60000);

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }



}