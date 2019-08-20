package is.yaks;

import io.zenoh.*;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Workspace to operate on Yaks.
 */
public class Workspace {

    private static final Logger LOG = LoggerFactory.getLogger("is.yaks");

    private static final short KIND_PUT = (short) 0x0;
    private static final short KIND_UPDATE = (short) 0x1;
    private static final short KIND_REMOVE = (short) 0x2;
    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocateDirect(0);

    private Path path;
    private Zenoh zenoh;
    private Map<Path, Storage> evals;


    protected Workspace(Path path, Zenoh zenoh) {
        this.path = path;
        this.zenoh = zenoh;
        this.evals = new Hashtable<Path, Storage>();
    }

    private Path toAsbsolutePath(Path p) {
        if (p.isRelative()) {
            return p.addPrefix(this.path);
        } else {
            return p;
        }
    }

    private Selector toAsbsoluteSelector(Selector s) {
        if (s.isRelative()) {
            return s.addPrefix(this.path);
        } else {
            return s;
        }
    }

    /**
     * Put a path/value into Yaks.
     * 
     * @param path the Path
     * @param value the value
     * @throws YException if put failed.
     */
    public void put(Path path, Value value) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Put on {} of {}", path, value);
        try {
            ByteBuffer data = value.encode();
            zenoh.writeData(path.toString(), data, value.getEncoding().getFlag(), KIND_PUT);
        } catch (ZException e) {
            throw new YException("Put on "+path+" failed", e);
        }
    }

    /**
     * Update a path/value into Yaks.
     * 
     * @param path the Path
     * @param value a delta to be applied on the existing value.
     * @throws YException if update failed.
     */
    public void update(Path path, Value value) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Update on {} of {}", path, value);
        try {
            ByteBuffer data = value.encode();
            zenoh.writeData(path.toString(), data, value.getEncoding().getFlag(), KIND_UPDATE);
        } catch (ZException e) {
            throw new YException("Update on "+path+" failed", e);
        }
    }

    /**
     * Remove a path/value from Yaks.
     * 
     * @param path the Path to be removed
     * @throws YException if remove failed.
     */
    public void remove(Path path) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Remove on {}", path);
        try {
            zenoh.writeData(path.toString(), EMPTY_BUF, (short)0, KIND_REMOVE);
        } catch (ZException e) {
            throw new YException("Remove on "+path+" failed", e);
        }
    }

    /**
     * Get a selection of path/value from Yaks.
     * 
     * @param selector the selector expressing the selection.
     * @return a collection of path/value.
     * @throws YException if get failed.
     */
    public Collection<PathValue> get(Selector selector) throws YException {
        final Selector s = toAsbsoluteSelector(selector);
        LOG.debug("Get on {}", s);
        try {
            final Collection<PathValue> results = new LinkedList<PathValue>();
            final java.util.concurrent.atomic.AtomicBoolean queryFinished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            
            zenoh.query(s.getPath(), s.getOptionalPart(),
                new ReplyCallback() {
                    public void handle(ReplyValue reply) {
                        switch (reply.getKind()) {
                            case Z_STORAGE_DATA:
                                Path path = new Path(reply.getRname());
                                ByteBuffer data = reply.getData();
                                short encodingFlag = (short) reply.getInfo().getEncoding();
                                LOG.debug("Get on {} => Z_STORAGE_DATA {} : {} bytes - encoding: {}", s, path, data.remaining(), encodingFlag);
                                try {
                                    Value value = Encoding.fromFlag(encodingFlag).getDecoder().decode(data);
                                    results.add(new PathValue(path, value));
                                } catch (YException e) {
                                    LOG.warn("Eval on {}: error decoding reply {} : {}", s, reply.getRname(), e);
                                }
                                break;
                            case Z_STORAGE_FINAL:
                                LOG.trace("Get on {} => Z_STORAGE_FINAL", s);
                                break;
                            case Z_REPLY_FINAL:
                                LOG.trace("Get on {} => Z_REPLY_FINAL => {} values received", s, results.size());
                                synchronized (results) {
                                    queryFinished.set(true);
                                    results.notify();
                                }
                                break;
                        }
                    }
                }
            );

            synchronized (results) {
                while (!queryFinished.get()) {
                    try {
                        results.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            return results;

        } catch (ZException e) {
            throw new YException("Get on "+selector+" failed", e);
        }
    }


    /**
     * Subscribe to a selection of path/value from Yaks.
     * 
     * @param selector the selector expressing the selection.
     * @param listener the Listener that will be called for each change of a path/value matching the selection.
     * @return a subscription id.
     * @throws YException if subscribe failed.
     */
    public SubscriptionId subscribe(Selector selector, Listener listener) throws YException {
        final Selector s = toAsbsoluteSelector(selector);
        LOG.debug("subscribe on {}", selector);
        try {
            Subscriber sub = zenoh.declareSubscriber(s.getPath(), SubMode.push(),
                new SubscriberCallback() {
                    public void handle(String rname, ByteBuffer data, DataInfo info) {
                        LOG.debug("subscribe on {} : received notif for {} (kind:{})", s, rname, info.getKind());
                        try {
                            // TODO: list of more than 1 change when available in zenoh-c
                            List<Change> changes = new ArrayList<Change>(1);

                            Path path = new Path(rname);
                            Change.Kind kind = Change.Kind.fromInt(info.getKind());
                            short encodingFlag = (short) info.getEncoding();
                            // TODO: timestamp when available in zenoh-c
                            long time = 0L;
                            try {
                                Value value = null;
                                if (kind != Change.Kind.REMOVE) {
                                    value = Encoding.fromFlag(encodingFlag).getDecoder().decode(data);
                                }
                                Change change = new Change(path, kind, time, value);
                                changes.add(change);
                            } catch (YException e) {
                                LOG.warn("subscribe on {}: error decoding change for {} : {}", s, rname, e);
                            }
                            listener.onChanges(changes);
                        } catch (Throwable e) {
                            LOG.warn("subscribe on {} : error receiving notification for {} : {}", s, rname, e);
                            LOG.debug("Stack trace: ", e);
                        }
                    }
                });
            return new SubscriptionId(sub);

        } catch (ZException e) {
            throw new YException("Subscribe on "+selector+" failed", e);
        }
    }

    /**
     * Unregisters a previous subscription.
     * 
     * @param the subscription id to unregister
     * @throws YException if unsubscribe failed.
     */
    public void unsubscribe(SubscriptionId subid) throws YException {
        // TODO when available in zenoh-c
    }

    private static final String ZENOH_EVAL_PREFIX = "+";
    private static final Resource[] EMPTY_EVAL_REPLY = new Resource[0];

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    /**
     * Registers an evaluation function under the provided path.
     * The function will be evaluated in a dedicated thread, and thus may call any other Workspace operation.
     * 
     * @param path the Path where the function can be triggered using {@link #eval(Selector)}
     * @param eval the evaluation function
     * @throws YException if registration failed.
     */
    public void registerEval(Path path, Eval eval) throws YException {
        final Path p = toAsbsolutePath(path);
        LOG.debug("registerEval on {}", p);
        try {
            Storage s = new Storage() {
                public void subscriberCallback(String rname, ByteBuffer data, DataInfo info) {
                    LOG.debug("Registered eval on {} received a publication on {}. Ignoer it!", p, rname);
                }

                public void queryHandler(String rname, String predicate, RepliesSender repliesSender) {
                    LOG.debug("Registered eval on {} handling query {}?{}", p, rname, predicate);
                    try {
                        Selector s = new Selector(rname+"?"+predicate);
                        THREAD_POOL.execute(new Runnable() {
                            @Override
                            public void run() {
                                Value v = eval.callback(path, predicateToProperties(s.getProperties()));
                                LOG.debug("Registered eval on {} handling query {}?{} returns: {}", p, rname, predicate, v);
                                repliesSender.sendReplies(
                                    new Resource[]{
                                        new Resource(path.toString(), v.encode(), v.getEncoding().getFlag(), Change.Kind.PUT.value())
                                    }
                                );
                            }
                        });
                    } catch (Throwable e) {
                        LOG.warn("Registered eval on {} caught an exception while handling query {} {} : {}", p, rname, predicate, e);
                        LOG.debug("Stack trace: ", e);
                        repliesSender.sendReplies(EMPTY_EVAL_REPLY);
                    }
                }
            };

            zenoh.declareStorage(ZENOH_EVAL_PREFIX + p.toString(), s);
            evals.put(p, s);

        } catch (ZException e) {
            throw new YException("registerEval on "+p+" failed", e);
        }

    }

    /**
     * Unregister a previously registered evaluation function.
     * 
     * @param path the path where the function has been registered
     * @throws YException if unregistration failed.
     */
    public void unregister_eval(Path path) throws YException {
        Storage s = evals.remove(path);
        if (s != null) {
            // TODO: remove storage when possible in zenoh-c
        }
    }

    /**
     * Requests the evaluation of registered evals whose registration path matches the given selector.
     * 
     * @param selector the selector
     * @return a collection of path/value where each value has been computed by the matching evaluation functions.
     * @throws YException if eval failed.
     */
    public Collection<PathValue> eval(Selector selector) throws YException {
        final Selector s = toAsbsoluteSelector(selector);
        LOG.debug("Eval on {}", s);
        try {
            final Collection<PathValue> results = new LinkedList<PathValue>();
            final java.util.concurrent.atomic.AtomicBoolean queryFinished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            
            zenoh.query(ZENOH_EVAL_PREFIX+s.getPath(), s.getOptionalPart(),
                new ReplyCallback() {
                    public void handle(ReplyValue reply) {
                        switch (reply.getKind()) {
                            case Z_STORAGE_DATA:
                                Path path = new Path(reply.getRname());
                                ByteBuffer data = reply.getData();
                                LOG.trace("Eval on {} => Z_STORAGE_DATA {} : {}", s, data);
                                short encodingFlag = (short) reply.getInfo().getEncoding();
                                try {
                                    Value value = Encoding.fromFlag(encodingFlag).getDecoder().decode(data);
                                    results.add(new PathValue(path, value));
                                } catch (YException e) {
                                    LOG.warn("Eval on {}: error decoding reply {} : {}", s, reply.getRname(), e);
                                }
                                break;
                            case Z_STORAGE_FINAL:
                                LOG.trace("Eval on {} => Z_STORAGE_FINAL", s);
                                break;
                            case Z_REPLY_FINAL:
                                LOG.trace("Eval on {} => Z_REPLY_FINAL => {} values received", s, results.size());
                                synchronized (results) {
                                    queryFinished.set(true);
                                    results.notify();
                                }
                                break;
                        }
                    }
                }
            );

            synchronized (results) {
                while (!queryFinished.get()) {
                    try {
                        results.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
            return results;

        } catch (ZException e) {
            throw new YException("Eval on "+selector+" failed", e);
        }

    }



    private static Properties predicateToProperties(String predicate) {
        Properties result = new Properties();
        String[] kvs = predicate.split(";");
        for (String kv : kvs) {
            int i = kv.indexOf('=');
            if (i > 0) {
                result.setProperty(kv.substring(0, i), kv.substring(i+1));
            }
        }
        return result;
    }

}
