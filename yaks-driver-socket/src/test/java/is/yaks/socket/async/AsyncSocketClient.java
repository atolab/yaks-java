package is.yaks.socket.async;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

import is.yaks.Encoding;
import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;
import is.yaks.socket.types.ObserverImpl;

public class AsyncSocketClient {

    private static AsyncYaks async_yaks;
    private static AsyncAdmin async_admin;
    private static AsyncWorkspace async_workspace;

    private static Observer observer;
    private static Observer eval_callback;
    private static Observer eval_callback2;

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws IOException, InterruptedException {

        // creating Yaks api
        System.out.println(">> Creating api");
        async_yaks = AsyncYaksImpl.getInstance();

        // initializing the observers
        observer = ObserverImpl.getInstance();
        // eval_callback = new ObserverImpl();
        // eval_callback2 = new ObserverImpl();

        // creating the connection properties
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

        // create workspace attached to the session
        System.out.println(">> creates workspace and adds subscriber");
        async_workspace = async_yaks.workspace(Path.ofString("/myyaks"));
        String subid = async_workspace.subscribe(YSelector.ofString("/myyaks/example/**"), observer);

        System.out.println(">> Put Tuple 1 - subid: " + subid);
        async_workspace.put(Path.ofString("/myyaks/example/one"), new Value("hello!", Encoding.STRING), quorum);
        // System.out.println("Called OBSERVER : "+listener.toString());

        System.out.println(">> Put Tuple 2");
        async_workspace.put(Path.ofString("/myyaks/example/two"), new Value("hello2!"), quorum);
        // System.out.println("Called OBSERVER : "+listener.toString());

        System.out.println(">> Put Tuple 3");
        async_workspace.put(Path.ofString("/myyaks/example/three"), new Value("hello3!"), quorum);
        // System.out.println("Called OBSERVER : "+listener.toString());

        System.out.println(">> Put Tuple JSON as RAW 4");
        Value d = new Value("{'this': 'is', 'a': 'json'}", Encoding.JSON);
        async_workspace.put(Path.ofString("/myyaks/example/four"), d, 1);
        // System.out.println("Called OBSERVER : "+listener.toString());

        System.out.println(">> Get Tuple 1");
        Map<Path, Value> kvs = async_workspace.get(YSelector.ofString("/myyaks/example/one"), 0);

        String strKey = "", strKey2 = "", strKey3 = "", strKey4 = "", strKey5 = "", strKey6 = "";
        String strValue = "", strValue2 = "", strValue3 = "", strValue4 = "", strValue5 = "", strValue6 = "";
        if (kvs != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs.entrySet()) {
                    strKey = entry.getKey().toString();
                    strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey + ", " + strValue + "]");

        System.out.println(">> Get Tuple 2");
        Map<Path, Value> kvs2 = async_workspace.get(YSelector.ofString("/myyaks/example"), 0);
        if (kvs2 != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs2.entrySet()) {
                    strKey2 = entry.getKey().toString();
                    strValue2 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs2.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey2 + ", " + strValue2 + "]");

        System.out.println(">> Get Tuple 3");
        Map<Path, Value> kvs3 = async_workspace.get(YSelector.ofString("/myyaks/example/*"), 0);
        if (kvs3 != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs3.entrySet()) {
                    strKey3 = entry.getKey().toString();
                    strValue3 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs3.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey3 + ", " + strValue3 + "]");

        System.out.println(">> Remove Tuple");
        System.out.println("REMOVE: [" + async_workspace.remove(Path.ofString("/myyaks/example/one"), quorum) + "]");

        System.out.println(">> Get Removed Tuple");
        Map<Path, Value> kvs4 = async_workspace.get(YSelector.ofString("/myyaks/example/one"), 0);
        if (kvs4 != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs4.entrySet()) {
                    strKey4 = entry.getKey().toString();
                    strValue4 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs4.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey4 + ", " + strValue4 + "]");

        System.out.println(">> Unsubscribe");
        if (subid != null && !subid.equals("")) {
            async_workspace.unsubscribe(subid);
        }

        System.out.println(">> Put Tuple"); // why this one returns values and not OK
        async_workspace.put(Path.ofString("/myyaks/example2/three"), new Value("hello3!", Encoding.STRING), quorum);

        System.out.println(">> Get Tuple");
        Map<Path, Value> kvs5 = async_workspace.get(YSelector.ofString("/myyaks/example/three"), 0);
        if (kvs5 != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs5.entrySet()) {
                    strKey5 = entry.getKey().toString();
                    strValue5 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs5.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey5 + ", " + strValue5 + "]");

        System.out.println(">> Create subscription without listener");
        String sid2 = async_workspace.subscribe(YSelector.ofString("/myyaks/example2/**"), observer);

        System.out.println(">> Put Tuple");
        async_workspace.put(Path.ofString("/myyaks/example2/three"), new Value("hello3!", Encoding.STRING), quorum);

        System.out.println(">> Get Tuple");
        System.out.println("GET: [" + async_workspace.get(YSelector.ofString("/myyaks/example2/three"), 0) + "]");

        System.out.println(">> Unsubscribe");
        if (sid2 != null && !sid2.equals("")) {
            async_workspace.unsubscribe(sid2);
        }

        System.out.println(">> Register Eval #1");
        async_workspace.register_eval(Path.ofString("/test_eval"), eval_callback);

        System.out.println(">> Register Eval #2");
        async_workspace.register_eval(Path.ofString("/test_eval2"), eval_callback2);

        // let%lwt _ = print_admin_space workspace in

        System.out.println(">> Get on Eval");
        Map<Path, Value> kvs6 = async_workspace.eval(YSelector.ofString("/myyaks/key1?(param=1)"), 0);

        if (kvs6 != null) {
            try {
                for (Map.Entry<Path, Value> entry : kvs6.entrySet()) {
                    strKey6 = entry.getKey().toString();
                    strValue6 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
                }
                kvs6.forEach((k, v) -> System.out.println("Item : " + k + " Value : " + v.getValue().toString()));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("GET: [" + strKey6 + ", " + strValue6 + "]");

        System.out.println(">> Unregister Eval");
        async_workspace.unregister_eval(Path.ofString("/myyaks/key1"));

        System.out.println(">> Dispose Storage");
        async_admin.remove_storage(stid, async_yaks);

        System.out.println(">> Close-logout");
        async_yaks.logout();
        System.out.println("bye!");

        // close Yaks api
        async_yaks.close();
    }
}
