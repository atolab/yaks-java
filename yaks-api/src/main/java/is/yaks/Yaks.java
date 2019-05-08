package is.yaks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yaks API entrypoint.
 */
public interface Yaks {

    public static int DEFAUL_PORT = 7887;

    public Logger logger = LoggerFactory.getLogger(Yaks.class);

    /**
     * Static operation to get an instance of a Yaks implementation.
     * 
     * @param yaksImplName
     *            Name of the class implementing the Yaks API
     * @param classLoader
     *            The Classloader to be used
     * @param args
     *            Options to pass to the Yaks implementation (e.g. credentials)
     * @return
     */
    public static Yaks getInstance(String yaksImplName, ClassLoader classLoader, String... args) {
        if (classLoader == null) {
            throw new NullPointerException("No class loader provided");
        }

        if (yaksImplName == null) {
            throw new NullPointerException("No Yaks implementation provided");
        }

        try {
            Class<?> yaks = classLoader.loadClass(yaksImplName);
            Constructor<?> ctor = yaks.getDeclaredConstructor(String[].class);
            ctor.setAccessible(true);
            return (Yaks) ctor.newInstance((Object) args);
        } catch (ClassNotFoundException e) {
            logger.error("{}", e);
        } catch (NoSuchMethodException e) {
            logger.error("{}", e);
        } catch (SecurityException e) {
            logger.error("{}", e);
        } catch (InstantiationException e) {
            logger.error("{}", e);
        } catch (IllegalAccessException e) {
            logger.error("{}", e);
        } catch (IllegalArgumentException e) {
            logger.error("{}", e);
        } catch (InvocationTargetException e) {
            logger.error("{}", e);
        }
        return null;
    }

    /**
     * Establish a session with the Yaks instance reachable through the provided *locator*.
     * 
     * Valid format for the locator are valid IP addresses as well as the combination IP:PORT.
     */
    public Yaks login(Properties properties);

    /**
     * Terminates this session.
     */
    public void logout();

    /**
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     * 
     */
    public Workspace workspace(Path path);

    /**
     * Creates an admin workspace that provides helper operations to administer Yaks.
     */
    public Admin admin();

    /**
     * Closes the Yaks api
     */
    public void close();

}
