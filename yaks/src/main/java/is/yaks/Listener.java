package is.yaks;

import java.util.List;

/**
 * Interface to be implemted for subscriptions (see {@link Workspace#subscribe(Selector, Listener)})
 */
public interface Listener {

    /**
     * The callback operation called for all changes on subscribed paths.
     * 
     * @param changes the list of changes.
     */
    public void onChanges(List<Change> changes);
}
