import is.yaks.*;

import java.util.Properties;
import java.util.Map;

public class YAddStorage {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String selector = "/demo/**";
        if (args.length > 1) {
            selector = args[1];
        }

        String storageId = "Demo";
        if (args.length > 2) {
            storageId = args[2];
        }

        try {
            Selector s = new Selector(selector);

            System.out.println("Login to "+locator+"...");
            Yaks y = Yaks.login(locator, null);

            Admin admin = y.admin();

            System.out.println("Add storage "+storageId+" with selector "+selector);
            Properties p = new Properties();
            p.setProperty("selector", selector);
            admin.addStorage(storageId, p);

            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}