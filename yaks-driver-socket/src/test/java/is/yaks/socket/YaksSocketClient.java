package is.yaks.socket;

import java.io.IOException;
import java.util.Properties;

import org.json.simple.JSONObject;

import is.yaks.Access;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Storage;
import is.yaks.Yaks;
import is.yaks.socket.YaksImpl;

public class YaksSocketClient 
{	
    private static Yaks yaks;
    private static Access access;
    private static Storage storage;
    private static Encoding encoding;

//    public static final Logger LOG = LoggerFactory.getLogger(AccessTest.class);
    
	@SuppressWarnings("static-access")
	public static void main(String[] args)  throws IOException, InterruptedException
	{
		//creating Yaks api
		System.out.println(">> Creating YAKS api");
		yaks =  YaksImpl.getInstance();
		
		//set properties host, port, cachesize
		Properties options = new Properties();
		options.setProperty("host", "localhost");
		options.setProperty("port", "7887");
		options.setProperty("cacheSize", "1024");
		
		//create storage
		System.in.read();
		storage = yaks.createStorage(Path.ofString("/fos"), options);
		
		
		//create access 
		System.in.read();
		access = yaks.createAccess(Path.ofString("/fos"), Long.parseLong(options.getProperty("cacheSize")), encoding.BYTE_BUFFER);
		
		
		//create subscription
		System.in.read();
		Long sid = access.subscribe(Selector.ofString("/fos/example/*"));
		
		
		//put tuple
		System.in.read();
		access.put(Selector.ofString("/fos/example/one"), "hello!");
		
		
		//put tuple
		System.in.read();
		access.put(Selector.ofString("/fos/example/two"), "hello2!");
		
		
		//put tuple
		System.in.read();
		access.put(Selector.ofString("/fos/example/three"), "hello3!");
		
		
		//put tuple JSON as RAW
		System.in.read();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("this", "is");
		jsonObj.put("a", "json");
		access.put(Selector.ofString("/fos/example/four"), jsonObj);
		
		
		//get tuple
		System.in.read();
		access.get(Selector.ofString("/fos/example/one"));
		
		
		//get tuple
		System.in.read();
		access.get(Selector.ofString("/fos/example"));
		
		
		//get tuple
		System.in.read();
		access.get(Selector.ofString("/fos/example/*"));
		
		
		//unsubscribe access
		System.in.read();
		access.unsubscribe(sid.toString());
		
		
		//dispose access
		System.in.read();
		access.dispose();

		//dispose storage
		System.in.read();
		storage.dispose();
	    
	    //close Yaks api
	    yaks.close();
	}
}
