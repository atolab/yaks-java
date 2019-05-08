package is.yaks;

import java.util.List;
import java.util.Properties;

import is.yaks.Path;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;

/**
 * 
 * Admin module for YAKS
 * 
 * ADLINK Technology Inc. - Yaks API refactoring
 *
 */
public interface Admin {

    public static String PREFIX = "@";
    public static String MY_YAKS = "local";

    public static Yaks yaks = null;
    public static Path path = null;
    public static Value value = null;
    public static Workspace ws = null;
    public static Properties properties = null;

    /**
     * Not supported in this version.
     * 
     */
    public boolean add_frontend(int feid, Properties properties, Yaks yaks);

    /**
     * Returns the frontend with the front-end ID **feid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_frontend(Yaks yaks);

    /**
     * Returns the list of frontends available on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_frontends(Yaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean remove_frontend(String feid, Yaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean add_backend(String beid, Properties properties, Yaks yaks);

    /**
     * Gets the back-end with id **beid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_backend(Yaks yaks);

    /**
     * Gets the list of all available back-ends on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_backends(String beid, Yaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public void remove_backend(String beid, Yaks yaks);

    /**
     * Adds a storage named **stid** on the backend **beid** and with storage and back-end specific configuration
     * defined through **properties**. The **properties** should always include the selector, e.g.,
     * *{"selector":"/demo/astore/**"}*. Main memory is the default backend used when **beid** is unset.
     * 
     * Finally, the storage is created on the Yaks instance with UUID **yaks**.
     * 
     * @return is_reply_ok
     */
    public void add_storage(String stid, Properties properties, String beid, Yaks yaks);

    /**
     * Gets the storage with id **stid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_storage(String stid, Yaks yaks);

    /**
     * Gets the list of all available storages on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_storages(String beid, Yaks yaks);

    /**
     * Removes the storage with id **stid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public boolean remove_storage(String stid, Yaks yaks);

    /**
     * Gets the list of all available sessions on the Yaks instance with UUID **yaks**
     * 
     */
    public List<Value> get_sessions(String feid, Yaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean close_session(String sid, Yaks yaks);

    /**
     * Gets the list of all active subscriptions on the Yaks instance with UUID **yaks**
     * 
     */
    public List<Value> get_subscriptions(String sid, Yaks yaks);

}
