import is.yaks.*;

import java.util.Properties;

public class YAddStorage {

    public static void main(String[] args) {
        String selector = "/demo/example/**";
        if (args.length > 0) {
            selector = args[0];
        }

        String storageId = "Demo";
        if (args.length > 1) {
            storageId = args[1];
        }

        String locator = null;
        if (args.length > 2) {
            locator = args[2];
        }

        try {
            System.out.println("Login to Yaks (locator="+locator+")...");
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
