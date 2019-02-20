package is.yaks.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import is.yaks.Access;
import is.yaks.Admin;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Storage;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class YaksImpl implements Yaks {


	private static Workspace workspace;
	private static SocketChannel sock;
	private static Selector selector;
	private static BufferedReader input = null;	

    private Map<String, Access> accessById = new HashMap<String, Access>();
    private Map<String, Storage> storageById = new HashMap<String, Storage>();

    private AdminImpl adminImpl;
    
    private YaksConfiguration config = YaksConfiguration.getInstance();
  
    private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();
    
    private static YaksImpl instance;
    Runtime rt = null;
    
    
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
    //        logger.error("Usage: <yaksUrl>");
            System.exit(-1);
        }
        String yaksUrl = args[0];
        if (yaksUrl.isEmpty()) {
            System.exit(-1);
        }

        config.setYaksUrl(yaksUrl);
    }
    
    public static SocketChannel getChannel() {
		return sock;
	}

 
    
    @Override
	public Yaks login(Properties properties) {
		Runtime rt = Runtime.getRuntime();
		int port;
    	String h = (String)properties.get("host");
    	String p = (String)properties.get("port");
    	if(p.equals("")) {
    		port = Yaks.DEFAUL_PORT;
    	} else {
    		port = Integer.parseInt(p);
    	}
    	try {
			// create non-blocking io socket
    		InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(h), port);
    		selector = Selector.open();
    		sock = SocketChannel.open(addr);
    		sock.setOption(StandardSocketOptions.TCP_NODELAY, true);
    		sock.configureBlocking(false);
    		sock.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    		
    		Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);
    		//write msg
    		loginM.write(sock, loginM);

    		System.in.read();
    		//read response msg
    		ByteBuffer buffer = ByteBuffer.allocate(1);
			sock.read(buffer);
			loginM.read(sock, buffer);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instance;
	}
    
    
    @SuppressWarnings("static-access")
	@Override
	public Admin admin() {
    	
    	if(sock.isConnected()) {

    		adminImpl = AdminImpl.getInstance();
    		
    		workspace = workspace(Path.ofString("/"+Admin.PREFIX+"/"+Admin.MY_YAKS));
    		
    		adminImpl.setWorkspace(workspace);
    	}
   	
    	return adminImpl;
	}
    
    
	/**
	 *  Creates a workspace relative to the provided **path**.
	        Any *put* or *get* operation with relative paths on this workspace
	        will be prepended with the workspace *path*.
	 */
    @Override
    public Workspace workspace(Path path) {
    	WorkspaceImpl wksp = new WorkspaceImpl();
    	try {
    		if(path != null) {
    			int wsid = 0;
    			
    			Message worskpaceM = new MessageFactory().getMessage(MessageCode.WORKSPACE, null);
    			worskpaceM.setPath(path);
    			if(sock.isConnected()) {
    				//post msg
    				worskpaceM.write(sock, worskpaceM);

    				System.in.read();
    				
    				//read response msg
    				ByteBuffer buffer = ByteBuffer.allocate(1);
    				sock.read(buffer);
    				Message msgReply = worskpaceM.read(sock, buffer);
    				//check_reply_is_ok
    				if(msgReply.getMessageCode().equals(MessageCode.OK)) {
    					//find_property wsid
    					Map<String, String> list = msgReply.getPropertiesList();
    					if(!list.isEmpty()) {
    						wsid = Integer.parseInt(list.get("wsid"));
    					}
    					wksp.setWsid(wsid);
    					wksp.setPath(path);
    				}
    			}
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return wksp;
    }

	@Override
	public void close() {
		try 
		{
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


	@Override
	public void logout() {
		
	}

	
}
