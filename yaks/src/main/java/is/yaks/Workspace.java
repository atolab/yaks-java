package is.yaks;

import io.zenoh.*;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
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
    private ExecutorService threadPool;
    private Map<Path, io.zenoh.Eval> evals;


    protected Workspace(Path path, Zenoh zenoh, ExecutorService threadPool) {
        this.path = path;
        this.zenoh = zenoh;
        this.threadPool = threadPool;
        this.evals = new Hashtable<Path, io.zenoh.Eval>();
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
     * @param path the {@link Path}
     * @param value the {@link Value}
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
     * @param path the {@link Path}
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
     * @param path the {@link Path} to be removed
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
     * @param selector the {@link Selector} expressing the selection.
     * @return a collection of path/value.
     * @throws YException if get failed.
     */
    public Collection<Entry> get(Selector selector) throws YException {
        final Selector s = toAsbsoluteSelector(selector);
        LOG.debug("Get on {}", s);
        try {
            final Map<Path, SortedSet<Entry>> map = new Hashtable<Path, SortedSet<Entry>>();
            final java.util.concurrent.atomic.AtomicBoolean queryFinished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            
            zenoh.query(s.getPath(), s.getOptionalPart(),
                new ReplyHandler() {
                    public void handleReply(ReplyValue reply) {
                        switch (reply.getKind()) {
                            case Z_STORAGE_DATA:
                            case Z_EVAL_DATA:
                                Path path = new Path(reply.getRname());
                                ByteBuffer data = reply.getData();
                                short encodingFlag = (short) reply.getInfo().getEncoding();
                                if (reply.getKind() == ReplyValue.Kind.Z_STORAGE_DATA) {
                                    LOG.debug("Get on {} => Z_STORAGE_DATA {} : {} ({} bytes)", s, path, reply.getInfo().getTimestamp(), data.remaining());
                                } else {
                                    LOG.debug("Get on {} => Z_EVAL_DATA {} : {} ({} bytes)", s, path, reply.getInfo().getTimestamp(), data.remaining());
                                }
                                try {
                                    Value value = Encoding.fromFlag(encodingFlag).getDecoder().decode(data);
                                    Entry entry = new Entry(path, value, reply.getInfo().getTimestamp());
                                    if (!map.containsKey(path)) {
                                        map.put(path, new TreeSet<Entry>());
                                    }
                                    map.get(path).add(entry);
                                } catch (YException e) {
                                    LOG.warn("Get on {}: error decoding reply {} : {}", s, reply.getRname(), e);
                                }
                                break;
                            case Z_STORAGE_FINAL:
                                LOG.trace("Get on {} => Z_STORAGE_FINAL", s);
                                break;
                            case Z_EVAL_FINAL:
                                LOG.trace("Get on {} => Z_EVAL_FINAL", s);
                                break;
                            case Z_REPLY_FINAL:
                                LOG.trace("Get on {} => Z_REPLY_FINAL", s);
                                synchronized (map) {
                                    queryFinished.set(true);
                                    map.notify();
                                }
                                break;
                        }
                    }
                }
            );

            synchronized (map) {
                while (!queryFinished.get()) {
                    try {
                        map.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }

            Collection<Entry> results = new LinkedList<Entry>();

            if (isSelectorForSeries(selector)) {
                // return all entries
                for (SortedSet<Entry> l : map.values()) {
                    for (Entry e : l) {
                        results.add(e);
                    }
                }
            } else {
                // return only the latest entry for each path
                for (SortedSet<Entry> l : map.values()) {
                    results.add(l.last());
                }
            }
            
            return results;

        } catch (ZException e) {
            throw new YException("Get on "+selector+" failed", e);
        }
    }

    private boolean isSelectorForSeries(Selector sel) {
        // search for starttime or stoptime property in selector
        String[] props = sel.getProperties().split(";");
        for (String p : props) {
            if (p.startsWith("starttime") || p.startsWith("stoptime")) {
                return true;
            } 
        }
        return false;
    }


    /**
     * Subscribe to a selection of path/value from Yaks.
     * 
     * @param selector the {@link Selector} expressing the selection.
     * @param listener the {@link Listener} that will be called for each change of a path/value matching the selection.
     * @return a {@link SubscriptionId}.
     * @throws YException if subscribe failed.
     */
    public SubscriptionId subscribe(Selector selector, Listener listener) throws YException {
        final Selector s = toAsbsoluteSelector(selector);
        LOG.debug("subscribe on {}", selector);
        try {
            Subscriber sub = zenoh.declareSubscriber(s.getPath(), SubMode.push(),
                new DataHandler() {
                    public void handleData(String rname, ByteBuffer data, DataInfo info) {
                        LOG.debug("subscribe on {} : received notif for {} (kind:{})", s, rname, info.getKind());
                        // TODO: list of more than 1 change when available in zenoh-c
                        List<Change> changes = new ArrayList<Change>(1);

                        try {
                            Path path = new Path(rname);
                            Change.Kind kind = Change.Kind.fromInt(info.getKind());
                            short encodingFlag = (short) info.getEncoding();
                            Timestamp timestamp= info.getTimestamp();
                            Value value = null;
                            if (kind != Change.Kind.REMOVE) {
                                value = Encoding.fromFlag(encodingFlag).getDecoder().decode(data);
                            }
                            Change change = new Change(path, kind, timestamp, value);
                            changes.add(change);
                        } catch (YException e) {
                            LOG.warn("subscribe on {}: error decoding change for {} : {}", s, rname, e);
                        }

                        if (threadPool != null) {
                            threadPool.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        listener.onChanges(changes);
                                    } catch (Throwable e) {
                                        LOG.warn("subscribe on {} : error receiving notification for {} : {}", s, rname, e);
                                        LOG.debug("Stack trace: ", e);
                                    }
                                }
                            });
                        }
                        else
                        {
                            try {
                                listener.onChanges(changes);
                            } catch (Throwable e) {
                                LOG.warn("subscribe on {} : error receiving notification for {} : {}", s, rname, e);
                                LOG.debug("Stack trace: ", e);
                            }
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
     * @param subid the {@link SubscriptionId} to unregister
     * @throws YException if unsusbscribe failed.
     */
    public void unsubscribe(SubscriptionId subid) throws YException {
        try {
            subid.getZSubscriber().undeclare();
        } catch (ZException e) {
            throw new YException("Unsubscribe failed", e);
        }
    }

    private static final Resource[] EMPTY_EVAL_REPLY = new Resource[0];

    /**
     * Registers an evaluation function under the provided {@link Path}.
     * The function will be evaluated in a dedicated thread, and thus may call any other Workspace operation.
     * 
     * @param path the {@link Path} where the function can be triggered using {@link #get(Selector)}
     * @param eval the evaluation function
     * @throws YException if registration failed.
     */
    public void registerEval(Path path, Eval eval) throws YException {
        final Path p = toAsbsolutePath(path);
        LOG.debug("registerEval on {}", p);
        try {
            QueryHandler qh = new QueryHandler() {
                public void handleQuery(String rname, String predicate, RepliesSender repliesSender) {
                    LOG.debug("Registered eval on {} handling query {}?{}", p, rname, predicate);
                    Selector s = new Selector(rname+"?"+predicate);
                    if (threadPool != null) {
                        threadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Value v = eval.callback(path, predicateToProperties(s.getProperties()));
                                    LOG.debug("Registered eval on {} handling query {}?{} returns: {}", p, rname, predicate, v);
                                    repliesSender.sendReplies(
                                        new Resource[]{
                                            new Resource(path.toString(), v.encode(), v.getEncoding().getFlag(), Change.Kind.PUT.value())
                                        }
                                    );
                                } catch (Throwable e) {
                                    LOG.warn("Registered eval on {} caught an exception while handling query {} {} : {}", p, rname, predicate, e);
                                    LOG.debug("Stack trace: ", e);
                                    repliesSender.sendReplies(EMPTY_EVAL_REPLY);
                                }
                            }
                        });
                    }
                    else
                    {
                        try {
                            Value v = eval.callback(path, predicateToProperties(s.getProperties()));
                            LOG.debug("Registered eval on {} handling query {}?{} returns: {}", p, rname, predicate, v);
                            repliesSender.sendReplies(
                                new Resource[]{
                                    new Resource(path.toString(), v.encode(), v.getEncoding().getFlag(), Change.Kind.PUT.value())
                                }
                            );
                        } catch (Throwable e) {
                            LOG.warn("Registered eval on {} caught an exception while handling query {} {} : {}", p, rname, predicate, e);
                            LOG.debug("Stack trace: ", e);
                            repliesSender.sendReplies(EMPTY_EVAL_REPLY);
                        }
                    }
                }
            };

            io.zenoh.Eval e = zenoh.declareEval(p.toString(), qh);
            evals.put(p, e);

        } catch (ZException e) {
            throw new YException("registerEval on "+p+" failed", e);
        }

    }

    /**
     * Unregister a previously registered evaluation function.
     * 
     * @param path the {@link Path} where the function has been registered
     * @throws YException if unregistration failed.
     */
    public void unregisterEval(Path path) throws YException {
        io.zenoh.Eval e = evals.remove(path);
        if (e != null) {
            try {
                e.undeclare();
            } catch (ZException ex) {
                throw new YException("unregisterEval failed", ex);
            }
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
