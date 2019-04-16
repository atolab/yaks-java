package is.yaks.socket.async;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.async.Admin;
import is.yaks.async.Workspace;
import is.yaks.async.Yaks;

public class AsyncSocketClient 
{	
	private static Yaks yaks;
	private static CompletableFuture<Workspace> workspaceFuture;
	private static CompletableFuture<Admin> adminFuture;

	private static Listener obs;   // TODO
	private static Listener evcb;  // TODO 


	@SuppressWarnings("static-access")
	public static void main(String[] args)  throws IOException, InterruptedException
	{
		try {
			//creating Yaks api
			System.out.println(">> Creating api");
			yaks =  YaksImpl.getInstance();

			//	listener = new Listener();
			String result = "";

			Properties properties = new Properties();
			properties.setProperty("host", "localhost");
			properties.setProperty("port", "7887");
			properties.setProperty("cacheSize", "1024");
			int quorum = 1;

			System.out.println(">> login");		
			yaks = yaks.login(properties);

			//creates an admin storage & workspace
			adminFuture = yaks.admin();
			Admin admin = adminFuture.get();

			System.out.println(">> create storage");		
			String stid = "demo";
			properties = new Properties();
			properties.setProperty("selector","/myyaks/**");

			admin.add_storage(stid, properties, "Memory", yaks);
			
			workspaceFuture = yaks.workspace(Path.ofString("/myyyaks/example"));
			Workspace workspace = workspaceFuture.get();

			workspace.put(Path.ofString("/myyaks/example/one"), new Value("hello!", Encoding.STRING), quorum);
			
			CompletableFuture<Map<Path, Value>> kvsFuture = workspace.get(Selector.ofString("/myyaks/example/one"), quorum);
			Map<Path, Value> kvs = kvsFuture.get();
			String strValue="";
			try {
				for (Map.Entry<Path, Value> entry : kvs.entrySet()) {
					strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
				}
				kvs.forEach((k,v)->System.out.println("Item : [" + k + "] Value : [" + v.getValue().toString()+"]"));

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			
			System.out.println(">> Close-logout");
			yaks.logout();
			System.out.println("bye!");

			//close Yaks api
			yaks.close();

		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}
}
