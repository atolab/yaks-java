package is.yaks.async;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Value;
import is.yaks.YSelector;

public interface Workspace {

    /**
     * The put operation:
     * 
     * - causes the notification of all subscriptions whose selector matches the path parameter, and
     * 
     * - stores the tuple <path,value> on all storages in YAKS whose selector matches the path parameter.
     * 
     * Notice that the **path** can be absolute or relative to the workspace.
     * 
     * If a **quorum** is provided then the put will success only if and only if a number quorum of independent storages
     * exist that match path. If such a set exist, the put operation will complete only after the tuple <path,value> has
     * been written on all these storages.
     * 
     * If no quorum is provided, then no assumptions are made and the **put** always succeeds, even if there are
     * currently no matching storage. In this case the only effect of this operation will be that of triggering matching
     * subscriber, if any exist.
     */

    public CompletableFuture<Boolean> put(Path p, Value v, int quorum);

    /**
     * Allows to **put** a delta, thus avoiding to distribute the entire value.
     */
    public CompletableFuture<Void> update();

    /**
     * gets the set of tuples *<path,value>* available in YAKS for which the path matches the selector, where the
     * selector can be absolute or relative to the workspace.
     * 
     * If a **quorum** is provided, then **get** will complete succesfully if and only if a number **quorum** of
     * independent and complete storage set exist. Complete storage means a storage that fully covers the selector (i.e.
     * any path matching the selector is covered by the storage). This ensures that if there is a *{ <path,value> }*
     * stored in YAKS for which the *path* matches the selector **s**, then there are at least **quorum** independent
     * copies of this element stored in YAKS. Of these **quorum** independent copies, the one returned to the
     * application is the most recent version.
     * 
     * If no quorum is provided (notice this is the default behaviour) then the [get] will succeed even if there isn't a
     * set of storages that fully covers the selector. I.e. storages that partially cover the selector will also reply.
     * 
     * The **encoding** allows an application to request values to be encoded in a specific format.
     * 
     * If no encoding is provided (this is the default behaviour) then YAKS will not try to perform any transcoding and
     * will return matching values in the encoding in which they are stored.
     * 
     * The **fallback** controls what happens for those values that cannot be transcoded into the desired encoding, the
     * available options are:
     * 
     * - Fail: the **get** fails if some value cannot be transcoded. - Drop: values that cannot be transcoded are
     * dropped. - Keep: values that cannot be transcoded are kept with their original encoding and left for the
     * application to deal with.
     */
    public CompletableFuture<Map<Path, Value>> get(YSelector selector, int quorum);

    /**
     * Removes from all Yaks's storages the tuples having the given **path**. The **path** can be absolute or relative
     * to the workspace. If a **quorum** is provided, then the *remove* will complete only after having successfully
     * removed the tuple from **quorum** storages.
     * 
     */
    public CompletableFuture<Boolean> remove(Path path, int quorum);

    /**
     * Registers a subscription to tuples whose path matches the **selector**.
     * 
     * A subscription identifier is returned. The **selector** can be absolute or relative to the workspace. If
     * specified, the **listener callback will be called for each **put** and **update** on tuples whose path matches
     * the subscription **selector**
     * 
     * @return sid subscriber_id
     */
    public CompletableFuture<String> subscribe(YSelector selector, Listener obs);

    /**
     * Subscribe without listener
     * 
     * @param subib
     * @return
     */
    public CompletableFuture<String> subscribe(YSelector selector);

    /**
     * Unregisters a previous subscription with the identifier **subid**
     */
    public CompletableFuture<Boolean> unsubscribe(String subid);

    /**
     * Registers an evaluation function **eval** under the provided **path**. The **path** can be absolute or relative
     * to the workspace.
     */
    public CompletableFuture<Boolean> register_eval(Path path, Listener evcb);

    /**
     * Unregisters an previously registered evaluation function under the give [path]. The [path] can be absolute or
     * relative to the workspace.
     */
    public CompletableFuture<Boolean> unregister_eval(Path path);

    /**
     * Requests the evaluation of registered evals whose registration **path** matches the given **selector**.
     * 
     * If several evaluation function are registered with the same path (by different Yaks clients), then Yaks will call
     * N functions where N=[multiplicity] (default value is 1). Note that in such case, the returned *{ <path,value> }*
     * will contain N time each matching path with the different values returned by each evaluation. The **encoding**
     * indicates the expected encoding of the resulting values. If the original values have a different encoding, Yaks
     * will try to transcode them into the expected encoding. By default, if no encoding is specified, the values are
     * returned with their original encoding. The **fallback** indicates the action that YAKS will perform if the
     * transcoding of a value fails.
     */
    public CompletableFuture<String> eval(YSelector selector);

    public int getWsid();

    public void setWsid(int id);

}
