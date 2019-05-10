package is.yaks.socket.async;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public class AsyncSocketClient {

    private static AsyncYaks async_yaks;
    private static AsyncAdmin async_admin;
    private static AsyncWorkspace async_workspace;

    private static Listener obs; // TODO
    private static Listener evcb; // TODO

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws IOException, InterruptedException {

        // creating Yaks api
        System.out.println(">> Creating api");
        async_yaks = AsyncYaksImpl.getInstance();

        // listener = new Listener();
        String result = "";

        Properties properties = new Properties();
        properties.setProperty("host", "localhost");
        properties.setProperty("port", "7887");
        properties.setProperty("cacheSize", "1024");
        int quorum = 1;

        System.out.println(">> login");
        async_yaks = async_yaks.login(properties);

        // creates an admin storage & workspace
        System.out.println(">> creates an admin");
        async_admin = async_yaks.admin();

        System.out.println(">> creates storage");
        String stid = "demo";
        properties = new Properties();
        properties.setProperty("selector", "/myyaks/**");
        async_admin.add_storage(properties, stid, "Memory");

        async_workspace = async_yaks.workspace(Path.ofString("/"));
        async_workspace.put(Path.ofString("/myyaks/example/one"), new Value("hello!", Encoding.STRING), quorum);

        Map<Path, Value> kvs = async_workspace.get(YSelector.ofString("/myyaks/example/one"), quorum);

        String strValue = "";
        try {
            for (Map.Entry<Path, Value> entry : kvs.entrySet()) {
                strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
            }
            kvs.forEach((k, v) -> System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString() + "]"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println(">> Close-logout");
        async_yaks.logout();
        System.out.println("bye!");

        // close Yaks api
        async_yaks.close();
    }
}
