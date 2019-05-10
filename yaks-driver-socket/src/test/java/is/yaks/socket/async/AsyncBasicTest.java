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
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public class AsyncBasicTest {

    private static AsyncYaks async_yaks;
    private static AsyncAdmin async_admin;
    private static AsyncWorkspace async_workspace;

    private Listener obs; // TODO
    private int quorum = 0;

    public static final Logger LOG = LoggerFactory.getLogger(AsyncBasicTest.class);

    @Before
    public void init() {
        Properties options = new Properties();
        options.setProperty("host", "localhost");
        options.setProperty("port", "7887");
        options.setProperty("cacheSize", "1024");

        System.out.println(">> [Client] Creating api");
        async_yaks = AsyncYaksImpl.getInstance();
        Assert.assertTrue(async_yaks instanceof AsyncYaksImpl);
    }

    @Test
    public void BasicTest() {
        // set properties host, port, cachesize
        Properties properties = new Properties();
        properties.setProperty("host", "localhost");
        properties.setProperty("port", "7887");
        properties.setProperty("cacheSize", "1024");

        async_yaks = AsyncYaksImpl.getInstance();

        async_yaks = async_yaks.login(properties);
        Assert.assertNotNull(async_yaks);

        // creates an admin
        async_admin = async_yaks.admin();
        Assert.assertNotNull(async_admin);

        // create storage
        String stid = "demo";
        properties = new Properties();
        properties.setProperty("selector", "/is.yaks.test/**");
        async_admin.add_storage(properties, stid, "Memory");

        // create workspace
        async_workspace = async_yaks.workspace(Path.ofString("/"));
        Assert.assertNotNull(async_workspace);

        String subid = async_workspace.subscribe(YSelector.ofString("/is.yaks.tests/*"), obs);
        Assert.assertNotNull(subid);

        boolean is_put_ok = async_workspace.put(Path.ofString("/is.yaks.tests/a"), new Value("ABC", Encoding.STRING),
                quorum);
        Assert.assertTrue(is_put_ok);

        Map<Path, Value> values = async_workspace.get(YSelector.ofString("/is.yaks.tests/a"), quorum);
        String strValue = "";
        try {
            for (Map.Entry<Path, Value> entry : values.entrySet()) {
                strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
            }
            values.forEach(
                    (k, v) -> System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString() + "]"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Assert.assertEquals("ABC", strValue);

        async_yaks.logout();

    }

    @After
    public void stop() {
        async_yaks.close();
    }

}