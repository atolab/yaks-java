package is.yaks.socket;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.MediaType;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import is.yaks.Access;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.socket.messages.MessageDelete;
import is.yaks.socket.messages.MessageGet;
import is.yaks.socket.messages.MessagePut;
import is.yaks.socket.messages.MessageSub;
import is.yaks.socket.messages.MessageUnsub;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.socketfe.EntityType;

public class AccessImpl implements Access {

	private static AccessImpl instance;
	private static SocketChannel channel;
	private static VLEEncoder encoder; 

    @Expose(serialize = true, deserialize = true)
    @SerializedName(value = "accessId", alternate = { "id" })
    private String accessId = "";

    @Expose(serialize = true, deserialize = true)
    @SerializedName(value = "alias")
    private String alias;

    @Expose(serialize = true, deserialize = true)
    @SerializedName(value = "path", alternate = { "scopePath" })
    private String scopePath;

    @Expose(serialize = true, deserialize = true)
    @SerializedName(value = "cacheSize", alternate = { "cache", "size" })
    private long cacheSize;

    // no modifier, only visible in class and package
    public AccessImpl(String accessId, Path scopePath, long cacheSize) {
        this(accessId, null, scopePath, cacheSize);
    }
    
 	private AccessImpl(){}
 	
 	public static synchronized Access getInstance() 
 	{
 		if( instance == null ) 
 		{
 			instance = new AccessImpl();
 		}
 		return instance;
 	}
 	
    AccessImpl(String accessId, String alias, Path scopePath, long cacheSize) {
        this.accessId = accessId;
        this.alias = alias;
        this.scopePath = scopePath.toString();
        this.cacheSize = cacheSize;
    }

    private YaksConfiguration getConfig() {
        return YaksConfiguration.getInstance();
    }    
    
    @Override
    public Access put(Selector selector, Object value) {
    	
    	channel = YaksImpl.getChannel();
    	MessagePut msgPutOne = new MessagePut(this.accessId, selector.toString(), value.toString());
    	
		try {
			channel.write(msgPutOne.write());
	
			//5.1 reading the 2 response messages from PUT
			int noOfmessages = 2;
			for(int i = 0; i < noOfmessages; i++) 
			{
				//get the size of the first msg
				ByteBuffer buff1 = ByteBuffer.allocate(1);
				channel.read(buff1);
				buff1.flip();
				int msgDataSize = (int) buff1.get();
				System.out.println((i+1) + " Message data size: " + msgDataSize);
				
				// get data
				buff1 = ByteBuffer.allocate(msgDataSize);
				channel.read(buff1.order(ByteOrder.BIG_ENDIAN));
				buff1.flip();  // make buffer ready for read
				msgPutOne.read(buff1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return instance;
    }

    @Override
    public Access deltaPut(Selector selector, Object delta) {
        /**
         * TBI:To Be Implemented.
         */
    	return null;
    }

    @Override
    public Access remove(Selector selector) {
    	/**
         * TBI:To Be Implemented.
         */
        return null;
    }

    @Override
    public Long subscribe(Selector selector) {
    	
    	System.out.println("::: Inside subscribe of AccessImpl ::: ");
    	
    	String sid = "";
    	channel = YaksImpl.getChannel();
    	MessageSub msgSub = new MessageSub(accessId, selector.toString());
    	
    	try {
			channel.write(msgSub.write());
	
			//4.1 reading the response to SUB
			int noOfmessages = 1;
			for(int i = 0; i < noOfmessages; i++) 
			{
				//get the size of the first msg
				ByteBuffer buff = ByteBuffer.allocate(1);
				channel.read(buff);
				buff.flip();
				int msgDataSize = (int) buff.get();
				System.out.println((i+1) + " Message data size: " + msgDataSize);
				
				// get data
				buff = ByteBuffer.allocate(msgDataSize);
				channel.read(buff.order(ByteOrder.BIG_ENDIAN));
				buff.flip();  // make buffer ready for read
				sid  = msgSub.subscribe(buff);
			}
        
    	} catch (IOException e) {
			e.printStackTrace();
		}
    	return Long.valueOf(sid);
    }

    @Override
    public <T> Long subscribe(Selector selector, Listener<T> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Selector> getSubscriptions() {
    	/**
         * TBI:To Be Implemented.
         */
    	return null;
    }

    @Override
    public void unsubscribe(String subscription_id) {
    	channel = YaksImpl.getChannel();
		MessageUnsub msgUnsub = new MessageUnsub(this.accessId, subscription_id);
		try {
			channel.write(msgUnsub.write());
		
			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			channel.read(byteBuffer);
			byteBuffer.flip();
			int msgDataSize = (int) byteBuffer.get();
			System.out.println(" Message data size: " + msgDataSize);
			
			// get data
			byteBuffer = ByteBuffer.allocate(msgDataSize);
			channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
			byteBuffer.flip();  // make buffer ready for read
			msgUnsub.read(byteBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public Value get(Selector selector) {
    	Value value = null;
    	
    	channel = YaksImpl.getChannel();
    	MessageGet msgGet = new MessageGet(this.accessId, selector.toString());
		try {
			channel.write(msgGet.write());
			
			int msgDataSize = 0 ;
			int noOfmessages = 1;
			
			for(int i = 0; i < noOfmessages; i++) 
			{
				//get the size of the first msg
				ByteBuffer byteBuffer = ByteBuffer.allocate(1);
				channel.read(byteBuffer);
				byteBuffer.flip();
				// read the vle correlation_id one byte at the time
				
				byte curByte = byteBuffer.get();				
				ByteBuffer bb = ByteBuffer.allocate(encoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				if ((curByte & encoder.MORE_BYTES_FLAG) == 0) {
					msgDataSize = (int) curByte;
				}
				else while((curByte & encoder.MORE_BYTES_FLAG) != 0) {
					bb.put((byte) (curByte));
					byteBuffer.clear();
					channel.read(byteBuffer);
					byteBuffer.flip();
					curByte = byteBuffer.get();
				}			
				bb.put(curByte);
				Utils.printHex(bb.array());
				msgDataSize = encoder.decode(bb.array()); // decode the msg_size id
				System.out.println((i+1) + " Message data size: " + msgDataSize);
				
				// get data
				byteBuffer = ByteBuffer.allocate(msgDataSize);
				channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
				byteBuffer.flip();  // make buffer ready for read
				value = msgGet.get(byteBuffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return value;
    }

    @Override
    public <T> Map<Path, T> get(Selector selector, Class<T> c) {
    	/**
         * TBI:To Be Implemented.
         */
    	return null;
    }

    @Override
    public <T> T get(Path path, Class<T> c) {
    	/**
         * TBI:To Be Implemented.
         */
    	return null;
    }

    @Override
    public void eval(Selector selector, Function<Path, Object> computation) {
        throw new UnsupportedOperationException();
        /**
         * TBI:To Be Implemented.
         */
    }

    @Override
    public void close() {
    	/**
         * TBI:To Be Implemented.
         */
    }

    @Override
    public void dispose() {
    	channel = YaksImpl.getChannel();
    	MessageDelete msgDeleteAccess = new MessageDelete(accessId, EntityType.ACCESS);
    	try {
			channel.write(msgDeleteAccess.write());

			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			channel.read(byteBuffer);
			
			byteBuffer.flip();
			int msgDataSize = (int) byteBuffer.get();
			System.out.println(" Message data size: " + msgDataSize);
			// get data
			byteBuffer = ByteBuffer.allocate(msgDataSize);
			channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
			byteBuffer.flip();  // make buffer ready for read
			msgDeleteAccess.read(byteBuffer);
	    	
    	} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public String getAccessId() {
        return accessId;
    }
	
	public void setAccessId(String accessId) {
		this.accessId = accessId;
	}
	
    public String getAlias() {
        return alias;
    }

    public String getScopePath() {
        return scopePath;
    }

    public long getCacheSize() {
        return cacheSize;
    }

	@Override
	public void unsubscribe(long subscriptionId) {
		/**
         * TBI:To Be Implemented.
         */
	}

}
