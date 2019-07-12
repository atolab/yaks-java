package is.yaks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.*;

/**
 * The Yaks client API.
 */
public class Yaks {

    private static final Logger LOG = LoggerFactory.getLogger("is.yaks");

    private Zenoh zenoh;
    private String yaksid;
    private Admin admin;

    private Yaks(Zenoh zenoh, String yaksid) {
        this.zenoh = zenoh;
        this.yaksid = yaksid;
        this.admin = new Admin(new Workspace(new Path("/@"), zenoh), yaksid);
    }


    private static Yaks initYaks(Zenoh z) throws YException {
        Properties props = z.info();
        String yaksid = props.getProperty("peer_pid");
        if (yaksid == null) {
            throw new YException("Failed to retrieve YaksId from Zenoh info");
        }
        LOG.info("Connected to Yaks {}", yaksid);
        return new Yaks(z, yaksid);
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
            Zenoh z = Zenoh.open(locator, username, password);
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
        return new Workspace(path, zenoh);
    }

    /**
     * Returns the Admin object.
     */
    public Admin admin() {
        return admin;
    }


}
