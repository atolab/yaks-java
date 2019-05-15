package is.yaks.socket.async;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Encoding;
import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;
import is.yaks.socket.types.ObserverImpl;

public class AsyncBasicTest {

    private static AsyncYaks async_yaks;
    private static AsyncAdmin async_admin;
    private static AsyncWorkspace async_workspace;

    private Observer obs;

    private int quorum = 0;

    public static final Logger LOG = LoggerFactory.getLogger(AsyncBasicTest.class);

    @Before
    public void init() {

        obs = ObserverImpl.getInstance();

        Properties properties = new Properties();
        properties.setProperty("host", "localhost");
        properties.setProperty("port", "7887");
        properties.setProperty("cacheSize", "1024");

        System.out.println(">> [Client] Creating api");
        async_yaks = AsyncYaksImpl.getInstance();
        Assert.assertTrue(async_yaks instanceof AsyncYaksImpl);

        async_yaks = async_yaks.login(properties);
        Assert.assertNotNull(async_yaks);

        // creates an admin
        System.out.println(">> [Client] admin ");
        async_admin = async_yaks.admin();
        Assert.assertNotNull(async_admin);

        // create storage
        String stid = "basic";
        properties = new Properties();
        properties.setProperty("selector", "/is.yaks.test/**");
        System.out.println(">> [Client] storage ");
        async_admin.add_storage(properties, stid, "Memory");

        // create workspace
        System.out.println(">> [Client] workspace ");
        async_workspace = async_yaks.workspace(Path.ofString("/"));
        Assert.assertNotNull(async_workspace);

        System.out.println(">> [Client] subscribe ");
        String subid = async_workspace.subscribe(YSelector.ofString("/is.yaks.tests/*"), obs);
        Assert.assertNotNull(subid);
    }

    @Test
    public void BasicTest() {

        System.out.println(">> [Client] Put ");
        boolean is_put_ok = async_workspace.put(Path.ofString("/is.yaks.tests/basic"),
                new Value("ABC", Encoding.STRING), quorum);
        Assert.assertTrue(is_put_ok);

        System.out.println(">> [Client] Get ");
        Map<Path, Value> kvs = async_workspace.get(YSelector.ofString("/is.yaks.tests/basic"), quorum);
        String strValue = "";
        try {
            for (Map.Entry<Path, Value> entry : kvs.entrySet()) {
                strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
            }
            kvs.forEach((k, v) -> System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString() + "]"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Assert.assertEquals("ABC", strValue);

        System.out.println(">> [Client] Remove ");
        boolean is_remove_ok = async_workspace.remove(Path.ofString("/is.yaks.tests/basic"), quorum);
        Assert.assertTrue(is_remove_ok);

        System.out.println(">> [Client] Get #2");
        Map<Path, Value> kvs2 = async_workspace.get(YSelector.ofString("/is.yaks.tests/basic"), quorum);
        String strValue2 = "";
        try {
            for (Map.Entry<Path, Value> entry : kvs2.entrySet()) {
                strValue2 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
            }
            kvs2.forEach((k, v) -> System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString() + "]"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(kvs2.isEmpty());

    }

    @After
    public void stop() {

        async_yaks.logout();

        async_yaks.close();
    }

}