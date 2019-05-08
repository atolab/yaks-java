package is.yaks.async;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Path;

/**
 * Yaks API entrypoint.
 */
public interface Yaks {

    public static int DEFAUL_PORT = 7887;

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
        assert classLoader != null;
        assert yaksImplName != null;
        try {
            Class<?> yaks = classLoader.loadClass(yaksImplName);
            Constructor<?> ctor = yaks.getDeclaredConstructor(String[].class);
            ctor.setAccessible(true);
            return (Yaks) ctor.newInstance((Object) args);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
     * Creates an admin workspace that provides helper operations to administer Yaks.
     */
    public CompletableFuture<Admin> admin();

    /**
     * Creates a workspace relative to the provided **path**. Any *put* or *get* operation with relative paths on this
     * workspace will be prepended with the workspace *path*.
     * 
     */
    public CompletableFuture<Workspace> workspace(Path path);

    /**
     * Closes the Yaks api
     */
    public void close();

    /**
     * Logout of the Yaks api
     */
    public void logout();

}
