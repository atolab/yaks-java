import is.yaks.*;

import java.io.InputStreamReader;
import java.util.List;

public class YSub    {

    public static void main(String[] args) {
        String s = "/demo/example/**";
        if (args.length > 0) {
            s = args[0];
        }

        String locator = null;
        if (args.length > 1) {
            locator = args[1];
        }

        try {
            Selector selector = new Selector(s);

            System.out.println("Login to Yaks (locator="+locator+")...");
            Yaks y = Yaks.login(locator, null);

            System.out.println("Use Workspace on '/'");
            Workspace w = y.workspace(new Path("/"));

            System.out.println("Subscribe on "+selector);
            SubscriptionId subid = w.subscribe(selector, 
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

            w.unsubscribe(subid);
            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }



}