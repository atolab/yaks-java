package is.yaks.socket.async;

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
import java.util.concurrent.ExecutionException;

import is.yaks.Message;
import is.yaks.Path;
import is.yaks.async.Admin;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;
import is.yaks.socket.messages.MessageFactory;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socket.utils.YaksConfiguration;
import is.yaks.utils.MessageCode;

public class YaksImpl implements Yaks {


	private CompletableFuture<Workspace> workspaceFuture;
	private CompletableFuture<Admin> adminFuture;
	private SocketChannel socketChannel;
	private static Selector selector;
	private static BufferedReader input = null;	

	public static final long TIMEOUT = 1l; // i.e 5l = 5ms, 1000l = i sec

	private AdminImpl adminImpl;

	private YaksConfiguration config = YaksConfiguration.getInstance();


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

	public SocketChannel getChannel() {
		return socketChannel;
	}



	@Override
	public Yaks login(Properties properties) {
//		Runtime rt = Runtime.getRuntime();
		int port;
		String h = (String)properties.get("host");
		String p = (String)properties.get("port");
		if(p.equals("")) {
			port = Yaks.DEFAUL_PORT;
		} else {
			port = Integer.parseInt((String)properties.get("port"));
		}
		try {
			InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(h), port);
			selector = Selector.open();
			socketChannel = SocketChannel.open(addr);
			socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			Message loginM = new MessageFactory().getMessage(MessageCode.LOGIN, properties);
			
			//write msg
			if(socketChannel!=null) {
				loginM.write(socketChannel, loginM);
				//==>
				int vle = 0;
				while(vle == 0) {
					vle = VLEEncoder.read_vle(socketChannel);
					Thread.sleep(TIMEOUT);
				} 
				if (vle > 0) {
					//	read response msg
					ByteBuffer buffer2 = ByteBuffer.allocate(vle);
					socketChannel.read(buffer2);
					loginM.read(buffer2);
				}
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
	public CompletableFuture<Admin> admin() {
		try {
			adminFuture = new CompletableFuture<Admin>();

			if(socketChannel.isConnected()) {

				adminImpl = AdminImpl.getInstance();

				workspaceFuture = workspace(Path.ofString("/"+Admin.PREFIX+"/"+Admin.MY_YAKS));

				adminImpl.setWorkspace(workspaceFuture.get());

			}
			adminFuture.complete((Admin)adminImpl);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return adminFuture;
	}


	/**
	 *  Creates a workspace relative to the provided **path**.
	        Any *put* or *get* operation with relative paths on this workspace
	        will be prepended with the workspace *path*.
	 */
	@Override
	public CompletableFuture<Workspace> workspace(Path path) {
		WorkspaceImpl ws = new WorkspaceImpl();
		CompletableFuture<Workspace> wsFuture = new CompletableFuture<Workspace>();
		try {
			if(path != null) 
			{
				int wsid = 0;

				Message worskpaceM = new MessageFactory().getMessage(MessageCode.WORKSPACE, null);
				worskpaceM.setPath(path);
				if(socketChannel.isConnected()) 
				{
					//post msg
					worskpaceM.write(socketChannel, worskpaceM);
					//==>
					int vle = 0;
					while(vle == 0) {
						vle = VLEEncoder.read_vle(socketChannel);
						Thread.sleep(TIMEOUT);
					} 
					if (vle > 0) {
						// read response msg
						System.out.println("==> [vle-workspace]:" +vle);
						ByteBuffer buffer = ByteBuffer.allocate(vle);
						socketChannel.read(buffer);
						Message msgReply = worskpaceM.read(buffer);

						if(msgReply.getMessageCode().equals(MessageCode.OK)) 
						{
							//find_property wsid
							Map<String, String> list = msgReply.getPropertiesList();
							if(!list.isEmpty()) {
								wsid = Integer.parseInt(list.get("wsid"));
							}
							ws.setWsid(wsid);
							ws.setPath(path);
							wsFuture.complete((Workspace)ws);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return wsFuture;
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

	}


}
