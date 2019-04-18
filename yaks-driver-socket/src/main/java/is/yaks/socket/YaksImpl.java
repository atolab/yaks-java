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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import is.yaks.Path;
import is.yaks.Admin;
import is.yaks.Message;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.utils.MessageCode;

public class YaksImpl implements Yaks {


	private static Workspace workspace;
	private SocketChannel socketChannel;
	private static Selector selector;
	private static BufferedReader input = null;	

	//public static final double TIMEOUT = 1; // i.e 5l = 5ms, 1000l = i sec
	
	private AdminImpl adminImpl;
	private YaksConfiguration config = YaksConfiguration.getInstance();
	private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();

	private static Yaks instance;
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

	public SocketChannel getChannel() {
		return socketChannel;
	}



	@Override
	public Yaks login(Properties properties) {
		Runtime rt = Runtime.getRuntime();
		int port;
		int vle = 0;
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
			socketChannel = SocketChannel.open(addr);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);
			//write msg
			loginM.write(socketChannel, loginM);
			//==>
			while(vle == 0) {
				vle = VLEEncoder.read_vle(socketChannel);
				Thread.sleep(1); // sleeps for 1 millisec
			} 
			if (vle > 0) {
				//	read response msg
				ByteBuffer buffer = ByteBuffer.allocate(vle);
				socketChannel.read(buffer);
				loginM.read(buffer);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return instance;
	}


	@SuppressWarnings("static-access")
	@Override
	public Admin admin() {

		if(socketChannel.isConnected()) {

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
		WorkspaceImpl ws = new WorkspaceImpl();
		int vle = 0;
		try {
			if(path != null) {
				int wsid = 0;

				Message workspaceM = new MessageFactory().getMessage(MessageCode.WORKSPACE, null);
				workspaceM.setPath(path);
				if(socketChannel.isConnected()) {
					//post msg
					workspaceM.write(socketChannel, workspaceM);
					//==>
					while(vle == 0) {
						vle = VLEEncoder.read_vle(socketChannel);
						Thread.sleep(1);
					}
					if (vle > 0) {
						//	read response msg
						ByteBuffer buffer = ByteBuffer.allocate(vle);
						socketChannel.read(buffer);
						Message msgReply = workspaceM.read(buffer);

						//check_reply_is_ok
						if(((Message) msgReply).getMessageCode().equals(MessageCode.OK)) {
							//find_property wsid
							Map<String, String> list = ((Message) msgReply).getPropertiesList();
							if(!list.isEmpty()) {
								wsid = Integer.parseInt(list.get("wsid"));
							}
							ws.setWsid(wsid);
							ws.setPath(path);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ws;
	}

	@Override
	public void close() {
		try 
		{
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void logout() {
		//:TBD
	}
}
