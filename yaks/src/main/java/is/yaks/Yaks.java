package is.yaks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.*;

/**
 * Yaks API.
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


    /**
     * Establish a session with the Yaks instance reachable through the provided *locator*.
     * 
     * Valid format for the locator are valid IP addresses as well as the combination IP:PORT.
     */
    public static Yaks login(String locator, Properties properties) throws YException {
        try {
            LOG.debug("Connecting to Zenoh on {}", locator);
            Zenoh z = Zenoh.open(locator);

            // TODO: get Zenoh peer_pid
            return new Yaks(z, "00000000-0000-0000-0000-000000000000");

        } catch (ZException e) {
            LOG.warn("Connection to Zenoh on {} failed", locator, e);
            throw new YException("Login failed to "+locator, e);
        }
    }

    /**
     * Terminates this session.
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
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     * 
     */
    public Workspace workspace(Path path) {
        return new Workspace(path, zenoh);
    }

    /**
     * Creates an admin workspace that provides helper operations to administer Yaks.
     */
    public Admin admin() {
        return admin;
    }


}
