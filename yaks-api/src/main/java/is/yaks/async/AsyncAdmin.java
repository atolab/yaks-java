package is.yaks.async;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Path;
import is.yaks.Value;

/**
 * 
 * Admin module for YAKS
 * 
 * ADLINK Technology Inc. - Yaks API refactoring
 *
 */
public interface AsyncAdmin {

    public static String PREFIX = "@";
    public static String MY_YAKS = "local";

    public static AsyncYaks yaks = null;
    public static Path path = null;
    public static Value value = null;
    public static AsyncWorkspace ws = null;
    public static Properties properties = null;

    /**
     * Not supported in this version.
     * 
     */
    public boolean add_frontend(int feid, Properties properties, AsyncYaks yaks);

    /**
     * Returns the frontend with the front-end ID **feid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_frontend(AsyncYaks yaks);

    /**
     * Returns the list of frontends available on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_frontends(AsyncYaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean remove_frontend(String feid, AsyncYaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean add_backend(String beid, Properties properties, AsyncYaks yaks);

    /**
     * Gets the back-end with id **beid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_backend(AsyncYaks yaks);

    /**
     * Gets the list of all available back-ends on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_backends(String beid, AsyncYaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public void remove_backend(String beid, AsyncYaks yaks);

    /**
     * Adds a storage named **stid** on the backend **beid** and with storage and back-end specific configuration
     * defined through **properties**. The **properties** should always include the selector, e.g.,
     * *{"selector":"/demo/astore/**"}*. Main memory is the default backend used when **beid** is unset.
     * 
     * Finally, the storage is created on the Yaks instance with UUID **yaks**.
     * 
     * @return is_reply_ok
     */
    public boolean add_storage(Properties properties, String stid, String beid);

    /**
     * Gets the storage with id **stid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public Value get_storage(String stid, AsyncYaks async_yaks);

    /**
     * Gets the list of all available storages on the Yaks instance with UUID **yaks**.
     * 
     */
    public List<Value> get_storages(String beid, AsyncYaks async_yaks);

    /**
     * Removes the storage with id **stid** on the Yaks instance with UUID **yaks**.
     * 
     */
    public boolean remove_storage(String stid, AsyncYaks async_yaks);

    /**
     * Gets the list of all available sessions on the Yaks instance with UUID **yaks**
     * 
     */
    public List<Value> get_sessions(String feid, AsyncYaks yaks);

    /**
     * Not supported in this version.
     * 
     */
    public boolean close_session(String sid, AsyncYaks yaks);

    /**
     * Gets the list of all active subscriptions on the Yaks instance with UUID **yaks**
     * 
     */
    public List<Value> get_subscriptions(String sid, AsyncYaks yaks);

}
