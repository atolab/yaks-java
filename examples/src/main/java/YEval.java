import is.yaks.*;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Properties;

public class YEval {

    public static void main(String[] args) {
        String locator = "tcp/127.0.0.1:7447";
        if (args.length > 0) {
            locator = args[0];
        }

        String p = "/demo/example/yaks-java-eval";
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
                    public Value callback(Path p, Properties properties) {
                        // In this Eval function, we choosed to get the name to be returned in the StringValue in 3 possible ways,
                        // depending the properties specified in the selector. For example, with the following selectors:
                        //   - "/demo/example/yaks-java-eval" : no properties are set, a default value is used for the name
                        //   - "/demo/example/yaks-java-eval?(name=Bob)" : "Bob" is used for the name
                        //   - "/demo/example/yaks-java-eval?(name=/demo/example/name)" :
                        //     the Eval function does a GET on "/demo/example/name" an uses the 1st result for the name

                        System.out.printf(">> Processing eval for path %s with properties: %s\n", p, properties);
                        String name = properties.getProperty("name", "Yaks Java!");

                        if (name.startsWith("/")) {
                            try {
                                System.out.printf("   >> Get name to use from Yaks at path: %s\n", name);
                                Collection<PathValue> kvs = w.get(new Selector(name));
                                if (!kvs.isEmpty()) {
                                    name = kvs.iterator().next().getValue().toString();
                                }
                            } catch (Throwable e) {
                                System.err.println("Failed to get value from path "+name);
                                e.printStackTrace();
                            }
                        }
                        System.out.printf("   >> Returning string: \"Eval from %s\"\n", name);
                        return new StringValue("Eval from "+name);
                    }
                }
            );

            System.out.println("Enter 'q' to quit...\n");
            InputStreamReader stdin = new InputStreamReader(System.in);
            while ((char) stdin.read() != 'q');

            w.unregisterEval(path);
            y.logout();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}