package is.yaks.socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

import is.yaks.Storage;
import is.yaks.socket.messages.MessageDelete;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.socketfe.EntityType;

public class StorageImpl implements Storage {

	private static StorageImpl instance;
	private static SocketChannel channel;
	
    private YaksConfiguration config = YaksConfiguration.getInstance();

    private String storageId;

    private String alias;    
    
    private StorageImpl(){}
 	
 	public static synchronized Storage getInstance() 
 	{
 		if( instance == null ) 
 		{
 			instance = new StorageImpl();
 		}
 		return instance;
 	}

    public StorageImpl(String storageId) {
        this(storageId, null);
    }

    public StorageImpl(String storageId, String alias) {
        this.storageId = storageId;
        this.alias = alias;
    }    
    
    @Override
    public void dispose() {
        assert storageId != null;
        channel = YaksImpl.getChannel();
        
        MessageDelete msgDeleteStorage = new MessageDelete(storageId, EntityType.STORAGE);
    	try {
	        channel.write(msgDeleteStorage.write());
			
			ByteBuffer buffer = ByteBuffer.allocate(1);
			channel.read(buffer);
			buffer.flip();
			int msgDataSize = (int) buffer.get();
			System.out.println(" Message data size: " + msgDataSize);
			
			// get data
			buffer = ByteBuffer.allocate(msgDataSize);
			channel.read(buffer.order(ByteOrder.BIG_ENDIAN));
			buffer.flip();  // make buffer ready for read
			msgDeleteStorage.read(buffer);
		
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
    
    public String getStorageId() {
        return storageId;
    }

    public String getAlias() {
        return alias;
    }

}
