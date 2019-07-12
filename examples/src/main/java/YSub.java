import is.yaks.*;

import java.util.List;

public class YSub    {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String s = "/demo/**";
        if (args.length > 1) {
            s = args[1];
        }

        try {
            Selector selector = new Selector(s);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Subscribe on "+selector);
            w.subscribe(selector, 
                new Listener() {
                    public void onChanges(List<Change> changes) {
                        for (Change c : changes) {
                            System.out.println(" -> "+c.getPath()+" : "+c.getValue());
                        }
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