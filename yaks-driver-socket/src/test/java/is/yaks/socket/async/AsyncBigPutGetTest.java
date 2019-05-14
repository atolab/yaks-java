package is.yaks.socket.async;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Observer;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;
import is.yaks.async.AsyncAdmin;
import is.yaks.async.AsyncWorkspace;
import is.yaks.async.AsyncYaks;

public class AsyncBigPutGetTest {

    private static AsyncYaks async_yaks;
    private static AsyncAdmin async_admin;
    private static AsyncWorkspace async_workspace;

    private Observer obs; // TODO
    private int quorum = 0;

    public static final Logger LOG = LoggerFactory.getLogger(AsyncBigPutGetTest.class);

    @Before
    public void init() {
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
        String stid = "demo";
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

    private static String create_data(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

    // @Test
    public void BigPutTest() {
        System.out.println(">> [Client] BigPutGetTest ");

        Value val_1k = new Value(create_data(1024));
        boolean is_put_ok = async_workspace.put(Path.ofString("/is.yaks.tests/big"), val_1k, quorum);
        Assert.assertTrue(is_put_ok);

        System.out.println(">> [Client] big_get ");
        Map<Path, Value> values2 = async_workspace.get(YSelector.ofString("/is.yaks.tests/big"), quorum);
        String strValue2 = "";
        try {
            for (Map.Entry<Path, Value> entry : values2.entrySet()) {
                strValue2 = new String(entry.getValue().getValue().getBytes(), "UTF-8");
            }
            values2.forEach(
                    (k, v) -> System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString() + "]"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(create_data(1024), strValue2);
    }

    @After
    public void stop() {

        async_yaks.logout();

        async_yaks.close();
    }

}