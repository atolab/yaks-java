package is.yaks.socket;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import is.yaks.Access;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Storage;
import is.yaks.Yaks;
import is.yaks.socket.messages.MessageCreate;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.socketfe.EntityType;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class YaksImpl implements Yaks {


	private Access access;
	private Storage storage;
	private static SocketChannel channel;

	private WebResource webResource;
    private Map<String, Access> accessById = new HashMap<String, Access>();
    private Map<String, Storage> storageById = new HashMap<String, Storage>();

    private YaksConfiguration config = YaksConfiguration.getInstance();
  
    private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();
    
    private static YaksImpl instance;
    
    
    private YaksImpl(){}
 	
 	public static synchronized Yaks getInstance() 
 	{
 		if( instance == null ) 
 		{
 			instance = new YaksImpl();
 		}
 		return instance;
 	}
 	
    private YaksImpl(String... args) {
        if (args.length == 0) {
            logger.error("Usage: <yaksUrl>");
            System.exit(-1);
        }
        String yaksUrl = args[0];
        if (yaksUrl.isEmpty()) {
            System.exit(-1);
        }

        config.setYaksUrl(yaksUrl);
        webResource = config.getClient().resource(config.getYaksUrl());

        registerShutdownHook();
    }
    
    public static SocketChannel getChannel() {
		return channel;
	}

	
    @Override
    public Access createAccess(Path scopePath, long cacheSize, Encoding encoding) {
    	System.out.println("::: Inside createAccess1 of YaksImpl ::: ");
        switch (encoding) {
        case JSON:
        default:
        	try {
        		ByteBuffer byteBuffer;
        		if (channel.isConnected()) {
	        		MessageCreate msgCreateAccess = new MessageCreate(EntityType.ACCESS, Path.ofString("//fos"), "", 1024, "", false);		
	        		channel.write(msgCreateAccess.write());			

        			byteBuffer = ByteBuffer.allocate(1);
        			channel.read(byteBuffer);
        			byteBuffer.flip();
        			int msgDataSize = (int) byteBuffer.get();

        			// get data
        			byteBuffer = ByteBuffer.allocate(msgDataSize);
        			channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
        			byteBuffer.flip();  // make buffer ready for read
        			access = msgCreateAccess.createAccess(byteBuffer);

        		} else {
        			Utils.fail("Yaks instance failed to connect to SocketChannel");
        			return null;
        		}
        	} catch (IOException e) {
        		Utils.fail("Yaks instance failed to connect to SocketChannel");
        		e.printStackTrace();
        	}
        }
    	return access;
    }

    @Override
    public Access createAccess(String id, Path path, long cacheSize, Encoding encoding) {
    	System.out.println("::: Inside createAccess2 of YaksImpl ::: ");
    	switch (encoding) {
        case JSON:
        default:
        	access = new AccessImpl(id, path, cacheSize);
        	return access;
        }
    }

    @Override
    public List<String> getAccess() {
        WebResource wr = webResource.path("/yaks/access");
        ClientResponse response = wr.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        String data = response.getEntity(String.class);
        if (response.getStatus() == HttpURLConnection.HTTP_OK) {
            List<String> idList = config.getGson().fromJson(data, gsonTypes.getCollectionTypeToken(String.class));
            return idList;
        } else {
            Utils.fail("Yaks instance failed to getAccess():\ncode: " + response.getStatus() + "\n" + "body: " + data);
            return null;
        }
    }

    @Override
    public Access getAccess(String id) {
        Access ret = accessById.get(id);
        if (ret != null) {
            return ret;
        }

        WebResource wr = webResource.path("/yaks/access/" + id);
        ClientResponse response = wr.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        String data = response.getEntity(String.class);
        switch (response.getStatus()) {
        case HttpURLConnection.HTTP_OK:
            AccessImpl access = config.getGson().fromJson(data, gsonTypes.getTypeToken(is.yaks.socket.AccessImpl.class));
            accessById.put(access.getAccessId(), access);
            if (access.getAlias() != null) {
                accessById.put(access.getAlias(), access);
            }
            return access;
        case HttpURLConnection.HTTP_NOT_FOUND:
        default:
            Utils.fail("Yaks instance failed to getAccess(" + id + "):\ncode: " + response.getStatus() + "\n" + "body: "
                    + data);
            return null;
        }
    }

    @Override
    public Storage createStorage(Path path, Properties option) {

    	System.out.println("::: Inside createStorage1 of YaksImpl ::: ");

    	InetSocketAddress inetAddr = new InetSocketAddress(option.getProperty("host"), Integer.parseInt(option.getProperty("port")));

    	try {
    		channel = SocketChannel.open(inetAddr);

    		ByteBuffer byteBuffer;
    		openConnection(channel);
    		if (channel.isConnected()) 
    		{
    			MessageCreate msgCreateStorage = new MessageCreate(EntityType.STORAGE,	path, "", 1024, "", false);
    			channel.write(msgCreateStorage.write());			

    			byteBuffer = ByteBuffer.allocate(1);
    			channel.read(byteBuffer);
    			byteBuffer.flip();
    			int msgDataSize = (int) byteBuffer.get();

    			// get data
    			byteBuffer = ByteBuffer.allocate(msgDataSize);
    			channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
    			byteBuffer.flip();  // make buffer ready for read
    			storage = msgCreateStorage.createStorage(byteBuffer);

    		} else {
    			Utils.fail("Yaks instance failed to connect to SocketChannel with host: "+ option.getProperty("host")+ ", port: "+option.getProperty("port"));
    			return null;
    		}
    	} catch (IOException e) {
    		Utils.fail("Yaks instance failed to open a SocketChannel with host: "+ option.getProperty("host")+ ", port: "+option.getProperty("port"));
    		e.printStackTrace();
    	}

    	return storage;
    }

    @Override
    public Storage createStorage(String id, Path path, Properties option) {
    	
       	System.out.println("::: Inside createStorage2 of YaksImpl ::: ");

    	storage = new StorageImpl(id, path.toString());

    	return storage;
    }

    @Override
    public List<String> getStorages() {
        WebResource wr = webResource.path("/yaks/storages");
        ClientResponse response = wr.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        if (response.getStatus() == HttpURLConnection.HTTP_OK) {
            List<String> idList = config.getGson().fromJson(response.getEntity(String.class),
                    gsonTypes.getCollectionTypeToken(String.class));

            return idList;
        } else {
            Utils.fail("Yaks instance failed to getStorage():\ncode: " + response.getStatus() + "\n" + "body: "
                    + response.getEntity(String.class));
            return null;
        }
    }

    @Override
    public Storage getStorage(String id) {
        Storage ret = storageById.get(id);
        if (ret != null) {
            return ret;
        }

        WebResource wr = webResource.path("/yaks/storages/" + id);
        ClientResponse response = wr.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        String data = response.getEntity(String.class);
        switch (response.getStatus()) {
        case HttpURLConnection.HTTP_OK:
            StorageImpl storage = config.getGson().fromJson(data,
                    gsonTypes.getTypeToken(is.yaks.socket.StorageImpl.class));
            if (storage == null) {
                throw new NullPointerException("Invalid null storage returned");
            }
            storageById.put(storage.getStorageId(), storage);
            if (storage.getAlias() != null) {
                storageById.put(storage.getAlias(), storage);
            }
            return storage;
        case HttpURLConnection.HTTP_NOT_FOUND:
        default:
            Utils.fail("Yaks instance failed to getStorage(" + id + "):\ncode: " + response.getStatus() + "\n"
                    + "body: " + data);
            return null;
        }
    }
    
    
    
    private void openConnection(SocketChannel channel) {
    	
    	MessageFactory messageFactory = new MessageFactory();
    	Message msgOpen = messageFactory.getMessage(MessageCode.OPEN);
		try {
			channel.write(msgOpen.write());			

			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			channel.read(byteBuffer);
			byteBuffer.flip();
			int msgDataSize = (int) byteBuffer.get();
			
			// get data
			byteBuffer = ByteBuffer.allocate(msgDataSize);
			channel.read(byteBuffer.order(ByteOrder.BIG_ENDIAN));
			byteBuffer.flip();  // make buffer ready for read
			msgOpen.read(byteBuffer);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
                YaksConfiguration.getInstance().getClient().destroy();
                YaksConfiguration.getInstance().getExecutorService().shutdown();
            }
        });
    }

	@Override
	public void close() {
		try 
		{
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
