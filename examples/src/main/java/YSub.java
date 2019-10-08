import is.yaks.*;

import java.io.InputStreamReader;
import java.util.List;

public class YSub    {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String s = "/demo/example/**";
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
                            switch (c.getKind()) {
                                case PUT:
                                    System.out.printf(">> [Subscription listener] Received PUT on '%s': '%s')\n", c.getPath(), c.getValue());
                                    break;
                                case UPDATE:
                                    System.out.printf(">> [Subscription listener] Received UPDATE on '%s': '%s')\n", c.getPath(), c.getValue());
                                    break;
                                case REMOVE:
                                    System.out.printf(">> [Subscription listener] Received REMOVE on '%s')\n", c.getPath());
                                    break;
                                default:
                                    System.err.printf(">> [Subscription listener] Received unkown operation with kind '%s' on '%s')\n", c.getKind(), c.getPath());
                                    break;
                            }
                        }
                    }
                }
            );

            System.out.println("Enter 'q' to quit...\n");
            InputStreamReader stdin = new InputStreamReader(System.in);
            while ((char) stdin.read() != 'q');

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }



}