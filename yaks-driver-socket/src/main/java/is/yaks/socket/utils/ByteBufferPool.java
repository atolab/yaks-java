package is.yaks.socket.utils;

import java.nio.ByteBuffer;
import java.util.List;

public interface ByteBufferPool {

    /** creates a new pool with at most n elements */
    public List<ByteBuffer>[] create(int n);

    public ByteBuffer create_bigstring(int length);

    public ByteBuffer create_bytes(int length);

    /** requests one free element of the pool p */
    public void use(ByteBufferPool p);

    /**
     * clear all elements in p, calling the dispose function associated with p on each of the cleared element
     */
    public void clear(ByteBufferPool p);

    /**
     * returns the number of requests currently waiting for an element of the pool p to become available.
     */
    public void wait_queue_length(ByteBufferPool p);

}
