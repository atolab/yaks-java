package is.yaks;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.ZException;
import io.zenoh.Zenoh;

/**
 * The Yaks client API.
 */
public class Yaks {

    private static final String PROP_USER = "user";
    private static final String PROP_PASSWORD = "password";

    private static final Logger LOG = LoggerFactory.getLogger("is.yaks");
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static String hexdump(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            sb.append(HEX_DIGITS[bytes[i] & 0x00F0 >>> 4]).append(HEX_DIGITS[bytes[i] & 0x000F]);
        }
        return sb.toString();
    }

    private Zenoh zenoh;
    private ExecutorService threadPool;
    private Admin admin;

    private Yaks(Zenoh zenoh, String yaksid) {
        this.zenoh = zenoh;
        this.threadPool = Executors.newCachedThreadPool();
        Workspace adminWs = new Workspace(new Path("/@"), zenoh, threadPool);
        this.admin = new Admin(adminWs, yaksid);
    }

    private static Yaks initYaks(Zenoh z) throws YException {
        Map<Integer, byte[]> props = z.info();
        byte[] yaksidb = props.get(Zenoh.INFO_PEER_PID_KEY);
        if (yaksidb == null) {
            throw new YException("Failed to retrieve YaksId from Zenoh info");
        }
        String yaksids = hexdump(yaksidb);
        LOG.info("Connected to Yaks {}", yaksids);
        return new Yaks(z, yaksids);
    }

    /**
     * Establish a session with the Yaks instance reachable via provided Zenoh
     * locator. If the provided locator is ``null``, {@link login} will perform some 
     * dynamic discovery and try to establish the session automatically. When not 
     * ``null``, the locator must have the format: {@code tcp/<ip>:<port>}.
     * 
     * @param locator    the Zenoh locator or ``null``.
     * @param properties the Properties to be used for this session (e.g. "user",
     *                   "password"...). Can be ``null``.
     * @return a {@link Yaks} object.
     * @throws YException if {@link login} fails.
     */
    public static Yaks login(String locator, Properties properties) throws YException {
        try {
            LOG.debug("Connecting to Yaks via Zenoh on {}", locator);
            Zenoh z;
            if (properties == null) {
                z = Zenoh.open(locator);
            } else {
                z = Zenoh.open(locator, getZenohProperties(properties));
            }
            return initYaks(z);

        } catch (ZException e) {
            LOG.warn("Connection Yaks via Zenoh on {} failed", locator, e);
            throw new YException("Login failed to " + locator, e);
        }
    }

    private static Map<Integer, byte[]> getZenohProperties(Properties properties) {
        Map<Integer, byte[]> zprops = new HashMap<Integer, byte[]>();

        if (properties.containsKey(PROP_USER))
            zprops.put(Zenoh.USER_KEY, properties.getProperty(PROP_USER).getBytes(UTF8));
        if (properties.containsKey(PROP_PASSWORD))
            zprops.put(Zenoh.PASSWD_KEY, properties.getProperty(PROP_PASSWORD).getBytes(UTF8));

        return zprops;
    }


    /**
     * Terminates the session with Yaks.
     */
    public void logout() throws YException {
        threadPool.shutdown();
        try {
            zenoh.close();
            this.zenoh = null;
        } catch (ZException e) {
            throw new YException("Error during logout", e);
        }
    }

    /**
     * Creates a Workspace using the provided path.
     * All relative {@link Selector} or {@link Path} used with this Workspace will be relative to this path.
     * <p>
     * Notice that all subscription listeners and eval callbacks declared in this workspace will be
     * executed by the I/O thread. This implies that no long operations or other call to Yaks
     * shall be performed in those callbacks.
     *
     * @param path the Workspace's path.
     * @return a {@link Workspace}.
     */
    public Workspace workspace(Path path) {
        return new Workspace(path, zenoh, null);
    }

    /**
     * Creates a Workspace using the provided path.
     * All relative {@link Selector} or {@link Path} used with this Workspace will be relative to this path.
     * <p>
     * Notice that all subscription listeners and eval callbacks declared in this workspace will be
     * executed by a CachedThreadPool. This is useful when listeners and/or callbacks need to perform
     * long operations or need to call other Yaks operations.
     *
     * @param path the Workspace's path.
     * @return a {@link Workspace}.
     */
    public Workspace workspaceWithExecutor(Path path) {
        return new Workspace(path, zenoh, threadPool);
    }

    /**
     * Returns the {@link Admin} object.
     */
    public Admin admin() {
        return admin;
    }

}
