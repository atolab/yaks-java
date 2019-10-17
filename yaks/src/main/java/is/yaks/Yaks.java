package is.yaks;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.*;

/**
 * The Yaks client API.
 */
public class Yaks {

    private static final Logger LOG = LoggerFactory.getLogger("is.yaks");
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static String hexdump(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            sb.append(HEX_DIGITS[bytes[i] & 0x00F0 >>> 4])
              .append(HEX_DIGITS[bytes[i] & 0x000F]);
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
     * Establish a session with the Yaks instance reachable via provided Zenoh locator.
     * The locator must have the format: tcp/<ip>:<port> .
     * 
     * @param locator the Zenoh locator.
     * @param properties unused in this version (can be null).
     * @return a Yaks object.
     * @throws YException if login fails.
     */
    public static Yaks login(String locator, Properties properties) throws YException {
        try {
            LOG.debug("Connecting to Yaks via Zenoh on {}", locator);
            Zenoh z = Zenoh.open(locator);
            return initYaks(z);

        } catch (ZException e) {
            LOG.warn("Connection Yaks via Zenoh on {} failed", locator, e);
            throw new YException("Login failed to "+locator, e);
        }
    }

    /**
     * Establish a session with the Yaks instance reachable via provided Zenoh locator
     * and using the specified user name and password.
     * The locator must have the format: tcp/<ip>:<port> .
     * 
     * @param locator the Zenoh locator.
     * @param username the user name.
     * @param password the password.
     * @return a Yaks object.
     * @throws YException if login fails.
     */
    public static Yaks login(String locator, String username, String password) throws YException {
        try {
            LOG.debug("Connecting to Yaks via Zenoh on {} with username: {}", locator, username);
            Map<Integer, byte[]> properties = new HashMap<Integer, byte[]>(2);
            properties.put(Zenoh.USER_KEY, "user".getBytes());
            properties.put(Zenoh.PASSWD_KEY, "password".getBytes());
            Zenoh z = Zenoh.open(locator, properties);
            return initYaks(z);

        } catch (ZException e) {
            LOG.warn("Connection Yaks via Zenoh on {} failed", locator, e);
            throw new YException("Login failed to "+locator, e);
        }
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
     * All relative {@link Selector} or {@Path} used with this Workspace will be relative to this path.
     *
     * @param path the Workspace's path.
     * @return a Workspace.
     */
    public Workspace workspace(Path path) {
        return new Workspace(path, zenoh, threadPool);
    }

    /**
     * Returns the Admin object.
     */
    public Admin admin() {
        return admin;
    }

}
