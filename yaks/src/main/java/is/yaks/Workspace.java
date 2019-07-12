package is.yaks;

import io.zenoh.*;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 *
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
    public void put(Path path, Value value) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Put on {} of {}", path, value);
        try {
            ByteBuffer data = value.encode();
            zenoh.writeData(path.toString(), data, value.getEncoding(), KIND_PUT);
        } catch (ZException e) {
            throw new YException("Put on "+path+" failed", e);
        }
    }

    public void update(Path path, Value value) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Update on {} of {}", path, value);
        try {
            ByteBuffer data = value.encode();
            zenoh.writeData(path.toString(), data, value.getEncoding(), KIND_UPDATE);
        } catch (ZException e) {
            throw new YException("Update on "+path+" failed", e);
        }
    }

    public void remove(Path path) throws YException {
        path = toAsbsolutePath(path);
        LOG.debug("Remove on {}", path);
        try {
            zenoh.writeData(path.toString(), EMPTY_BUF, (short)0, KIND_REMOVE);
        } catch (ZException e) {
            throw new YException("Remove on "+path+" failed", e);
        }
    }

    public Collection<PathValue> get(Selector selector) throws YException {
        selector = toAsbsoluteSelector(selector);
        LOG.debug("Get on {}", selector);
        try {
            final Collection<PathValue> results = new LinkedList<PathValue>();
            final java.util.concurrent.atomic.AtomicBoolean queryFinished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            
            zenoh.query(selector.getPath(), selector.getOptionalPart(),
                new ReplyCallback() {
                    public void handle(ReplyValue reply) {
                        switch (reply.getKind()) {
                            case Z_STORAGE_DATA:
                                Path path = new Path(reply.getRname());
                                ByteBuffer data = reply.getData();
                                LOG.trace("Get on {} => Z_STORAGE_DATA {} : {}", path, data);
                                // TODO: encoding
                                results.add(new PathValue(path, StringValue.Decoder.decode(data)));
                                break;
                            case Z_STORAGE_FINAL:
                                LOG.trace("Get on {} => Z_STORAGE_FINAL");
                                break;
                            case Z_REPLY_FINAL:
                                LOG.trace("Get on {} => Z_REPLY_FINAL => {} values received", results.size());
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


    public SubscriptionId subscribe(Selector selector, Listener listener) throws YException {
        final Selector sel = toAsbsoluteSelector(selector);
        LOG.debug("subscribe on {}", selector);
        try {
            Subscriber sub = zenoh.declareSubscriber(sel.getPath(), SubMode.push(),
                new SubscriberCallback() {
                    public void handle(String rname, ByteBuffer data, DataInfo info) {
                        LOG.debug("subscribe on {} : received notif for {} (kind:{})", sel, rname, info.getKind());
                        try {
                            Path path = new Path(rname);
                            Change.Kind kind = Change.Kind.fromInt(info.getKind());
                            // TODO: timestamp when available in zenoh-c
                            long time = 0L;
                            // TODO: encoding
                            Value value = (kind == Change.Kind.REMOVE ? null : StringValue.Decoder.decode(data));
                            Change change = new Change(path, kind, time, value);
                            // TODO: list of changes when available in zenoh-c
                            List<Change> changes = new ArrayList<Change>(1);
                            changes.add(change);
                            listener.onChanges(changes);
                        } catch (Throwable e) {
                            LOG.warn("subscribe on {} : error receiving notification for {} : {}", sel, rname, e);
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
     * Unregisters a previous subscription with the identifier **subid**
     */
    public void unsubscribe(SubscriptionId subid) throws YException {
        // TODO when available in zenoh-c
    }

    private static final String ZENOH_EVAL_PREFIX = "+";
    private static final Resource[] EMPTY_EVAL_REPLY = new Resource[0];

    /**
     * Registers an evaluation function **eval** under the provided **path**. The **path** can be absolute or relative
     * to the workspace.
     */
    public void registerEval(Path path, Eval eval) throws YException {
        final Path p = toAsbsolutePath(path);
        LOG.debug("registerEval on {}", p);
        try {
            Storage s = new Storage() {
                public void subscriberCallback(String rname, ByteBuffer data, DataInfo info) {
                    LOG.debug("Registered eval on {} received a publication on {}. Ignoer it!", p, rname);
                }

                public Resource[] queryHandler(String rname, String predicate) {
                    LOG.debug("Registered eval on {} handling query {}?{}", p, rname, predicate);
                    try {
                        Selector s = new Selector(rname+"?"+predicate);
                        Value v = eval.callback(path, predicateToProperties(s.getProperties()));
                        LOG.debug("Registered eval on {} return value: {}", p, v);
                        return new Resource[]{
                            new Resource(path.toString(), v.encode(), v.getEncoding(), Change.Kind.PUT.value())
                        };
                    } catch (Throwable e) {
                        LOG.warn("Registered eval on {} caught an exception while handling query {} {} : {}", p, rname, predicate, e);
                        LOG.debug("Stack trace: ", e);
                       return EMPTY_EVAL_REPLY;
                    }
                }

                public void repliesCleaner(Resource[] replies) {
                    // do nothing
                }
            };

            zenoh.declareStorage(ZENOH_EVAL_PREFIX + p.toString(), s);
            evals.put(p, s);

        } catch (ZException e) {
            throw new YException("registerEval on "+p+" failed", e);
        }

    }

    public void unregister_eval(Path path) throws YException {
        Storage s = evals.remove(path);
        if (s != null) {
            // TODO: remove storage when possible in zenoh-c
        }
    }

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
    public Collection<PathValue> eval(Selector selector) throws YException {
        selector = toAsbsoluteSelector(selector);
        LOG.debug("Eval on {}", selector);
        try {
            final Collection<PathValue> results = new LinkedList<PathValue>();
            final java.util.concurrent.atomic.AtomicBoolean queryFinished =
                new java.util.concurrent.atomic.AtomicBoolean(false);
            
            zenoh.query(ZENOH_EVAL_PREFIX+selector.getPath(), selector.getOptionalPart(),
                new ReplyCallback() {
                    public void handle(ReplyValue reply) {
                        switch (reply.getKind()) {
                            case Z_STORAGE_DATA:
                                Path path = new Path(reply.getRname());
                                ByteBuffer data = reply.getData();
                                LOG.trace("Eval on {} => Z_STORAGE_DATA {} : {}", path, data);
                                // TODO: encoding
                                results.add(new PathValue(path, StringValue.Decoder.decode(data)));
                                break;
                            case Z_STORAGE_FINAL:
                                LOG.trace("Eval on {} => Z_STORAGE_FINAL");
                                break;
                            case Z_REPLY_FINAL:
                                LOG.trace("Eval on {} => Z_REPLY_FINAL => {} values received", results.size());
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