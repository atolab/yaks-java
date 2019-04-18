package is.yaks.socket.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import is.yaks.utils.ByteBufferPool;

public class ByteBufferPoolImpl implements ByteBufferPool {
	
	private ByteBuffer buffer;
	private int offset; 
	private int capacity; 
	private int grow;
	
	private final int max_buffer_count = 32;
	
	private final int max_buffer_size = 64 * 1024;
			
	final List<ByteBuffer>[] potBuffers;
	
	
	@SuppressWarnings("unchecked")
	public ByteBufferPoolImpl(){
		potBuffers = (List<ByteBuffer>[]) new List[max_buffer_count];
		for (List<ByteBuffer> bb : potBuffers) {			
			bb = new ArrayList<ByteBuffer>(max_buffer_size);
		}
	}
	
	/** creates a new pool with at most n elements */
	@Override
	public ByteBuffer create(int n) {
    
    	return null;
    }
    
    /** requests one free element of the pool p */
    @Override
    public void use(ByteBufferPool p) {
    	
    }
    
    /** clear all elements in p, calling the dispose function 
     * associated with p on each of the cleared element */
    @Override
    public void clear(ByteBufferPool p) {
    	
    }
    
    /** returns the number of requests currently waiting 
     * for an element of the pool p to become available.*/
    @Override
    public void wait_queue_length(ByteBufferPool p) {
    	
    }

	@Override
	public ByteBuffer create_bigstring(int length) {
		return null;
	}

	@Override
	public ByteBuffer create_bytes(int length) {
		return null;
	}
    	
}
