package is.yaks.socket;

import java.io.IOException;
import java.util.Properties;

import is.yaks.Admin;
import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;

public class SocketClient 
{	
    private static Yaks yaks;
    private static Workspace workspace;
    private static Admin admin;

	private static Listener obs;   // TODO
	private static Listener evcb;  // TODO 
	
    
	@SuppressWarnings("static-access")
	public static void main(String[] args)  throws IOException, InterruptedException
	{
		//creating Yaks api
		System.out.println(">> Creating api");
		yaks =  YaksImpl.getInstance();

	//	listener = new Listener();
		
		Properties properties = new Properties();
		properties.setProperty("host", "localhost");
		properties.setProperty("port", "7887");
		properties.setProperty("cacheSize", "1024");
		int quorum = 1;
		
		System.out.println(">> login");		
		yaks = yaks.login(properties);
		
		//creates an admin storage & workspace
		admin = yaks.admin();
		System.out.println(">> create storage");		
		//System.in.read();
		String stid = "demo";
		properties = new Properties();
		properties.setProperty("selector","/myyaks/**");
		admin.add_storage(stid, properties, "Memory", yaks);
		
		// create workspace attached to the session
		System.out.println(">> create workspace and subscription"); 
		workspace = yaks.workspace(Path.ofString("/myyaks"));	
		String subid = workspace.subscribe(Selector.ofString("/myyaks/example/**"), obs);

		System.out.println(">> Put Tuple 1 - subid: "+ subid);
		workspace.put(Path.ofString("/myyaks/example/one"), new Value("hello!", Encoding.STRING), quorum);
//		System.out.println("Called OBSERVER : "+listener.toString());
		
		System.out.println(">> Put Tuple 2");
		workspace.put(Path.ofString("/myyaks/example/two"), new Value("hello2!"), quorum);
//		System.out.println("Called OBSERVER : "+listener.toString());
		
		System.out.println(">> Put Tuple 3");
		workspace.put(Path.ofString("/myyaks/example/three"), new Value("hello3!"), quorum);
//		System.out.println("Called OBSERVER : "+listener.toString());
		
		System.out.println(">> Put Tuple JSON as RAW 4");
		Value d = new Value("{'this': 'is', 'a': 'json'}", Encoding.JSON);
		workspace.put(Path.ofString("/myyaks/example/four"), d, 1);
//		System.out.println("Called OBSERVER : "+listener.toString());
		
		System.out.println(">> Get Tuple 1");
		System.out.println("GET: [" + workspace.get(Selector.ofString("/myyaks/example/one"))+"]");

		System.out.println(">> Get Tuple 2");
		System.out.println("GET: ["+ workspace.get(Selector.ofString("/myyaks/example"))+"]");

		System.out.println(">> Get Tuple 3");
		System.out.println("GET: [" + workspace.get(Selector.ofString("/myyaks/example/*"))+"]");
		
		System.out.println(">> Remove Tuple");
		System.out.println("REMOVE: ["+ workspace.remove(Path.ofString("/myyaks/example/one"), quorum)+"]");

		System.out.println(">> Get Removed Tuple");
		System.out.println("GET: ["+ workspace.get(Selector.ofString("/myyaks/example/one"))+"]");

		System.out.println(">> Unsubscribe");
	    if(subid != null && !subid.equals("")) {
	        workspace.unsubscribe(subid);
	    }

	    System.out.println(">> Put Tuple");
	    workspace.put(Path.ofString("/myyaks/example2/three"), new Value("hello3!", Encoding.STRING), quorum);

	    System.out.println(">> Get Tuple");
	    System.out.println("GET: [" + workspace.get(Selector.ofString("/myyaks/example/three"))+"]");
	    
	    System.out.println(">> Create subscription without listener");
	    String sid2 = workspace.subscribe(Selector.ofString("/myyaks/example2/**"));

	    System.out.println(">> Put Tuple");
	    workspace.put(Path.ofString("/myyaks/example2/three"), new Value("hello3!", Encoding.STRING), quorum);

	    System.out.println(">> Get Tuple");
	    System.out.println("GET: ["+workspace.get(Selector.ofString("/myyaks/example2/three"))+"]");

	    System.out.println(">> Unsubscribe");
	    if (sid2 != null && !sid2.equals("")) {
	        workspace.unsubscribe(sid2);
	    }
	        
	    System.out.println(">> Register Eval");
	    workspace.register_eval(Path.ofString("/myyaks/key1"), evcb);

	    System.out.println(">> Get on Eval");
	    System.out.println("GET: [" + workspace.eval(Selector.ofString("/myyaks/key1?(param=1)"))+"]");

	    System.out.println(">> Unregister Eval");
	    workspace.unregister_eval(Path.ofString("/myyaks/key1"));

	    System.out.println(">> Dispose Storage");
	    admin.remove_storage(stid, yaks);
	    
	    System.out.println(">> Close-logout");
	    yaks.logout();
	    System.out.println("bye!");
	    
	    //close Yaks api
	    yaks.close();
	    
	    
	    
	    
	}
}
