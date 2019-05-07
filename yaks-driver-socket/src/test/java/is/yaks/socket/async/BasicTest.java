package is.yaks.socket.async;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.async.Admin;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.YaksConfiguration;

public class BasicTest {

    private Yaks yaks;
    private CompletableFuture<Admin> adminFuture;
    private CompletableFuture<Workspace> workspaceFuture;

    private Listener obs; // TODO
    private int quorum = 0;

    public static final Logger LOG = LoggerFactory.getLogger(BasicTest.class);
    private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();

    @Before
    public void init() {
        String[] args = { "http://localhost:7887" };
        yaks = Yaks.getInstance("is.yaks.socket.async.YaksImpl", BasicTest.class.getClassLoader(), args);
        Assert.assertTrue(yaks instanceof YaksImpl);
    }

    // @Test
    public void BasicTest() {
        // set properties host, port, cachesize
        Properties options = new Properties();
        options.setProperty("host", "localhost");
        options.setProperty("port", "7887");
        options.setProperty("cacheSize", "1024");
        try {
            yaks = YaksImpl.getInstance();
            yaks.login(options);
            Assert.assertNotNull(yaks);

            // creates an admin
            adminFuture = yaks.admin();
            Admin admin = adminFuture.get();
            Assert.assertNotNull(admin);

            // create storage
            String stid = "demo";
            options = new Properties();
            options.setProperty("selector", "/is.yaks.test/**");
            admin.add_storage(stid, options, "Memory", yaks);

            // create workspace
            workspaceFuture = yaks.workspace(Path.ofString("/"));
            Workspace workspace = workspaceFuture.get();
            Assert.assertNotNull(workspace);

            CompletableFuture<String> subidFuture = workspace.subscribe(Selector.ofString("/is.yaks.tests/*"), obs);
            String subid = subidFuture.get();
            Assert.assertNotNull(subid);

            // put simple tuple
            CompletableFuture<Boolean> isPutOKFuture = workspace.put(Path.ofString("/is.yaks.tests/a"),
                    new Value("ABC", Encoding.STRING), quorum);
            boolean isOk = isPutOKFuture.get();
            Assert.assertTrue(isOk);

            // get object Value from key
            CompletableFuture<Map<Path, Value>> valuesFuture = workspace.get(Selector.ofString("/is.yaks.tests/a"),
                    quorum);
            Map<Path, Value> values = valuesFuture.get();

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

            yaks.logout();

        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } catch (ExecutionException e1) {
            e1.printStackTrace();
        }

    }

    @After
    public void stop() {
        yaks.close();
    }

}