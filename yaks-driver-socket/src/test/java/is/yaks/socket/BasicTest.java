package is.yaks.socket;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Admin;
import is.yaks.Encoding;
import is.yaks.Listener;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.YaksConfiguration;

public class BasicTest {

	private Yaks yaks;
	private Admin admin;
	private Workspace workspace;
	
	private Listener obs;   // TODO

	public static final Logger LOG = LoggerFactory.getLogger(BasicTest.class);
    private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();

	@Before
	public void init() {
		String[] args = { "http://localhost:7887" };
		yaks = Yaks.getInstance("is.yaks.socket.async.YaksImpl", BasicTest.class.getClassLoader(), args);
		Assert.assertTrue(yaks instanceof YaksImpl);
	}

	@Test
	public void debugTest() {
		YaksConfiguration config = YaksConfiguration.getInstance();
	
		Map<Path, String> map = new HashMap<>();
		map.put(Path.ofString("/is.yaks.socket.tests/a"), "ABC");
		map.put(Path.ofString("/is.yaks.socket.tests/x"), "XYZ");
		String json = config.getGson().toJson(map);
		  
	    Map<Path, String> result = new HashMap<>();
        result = config.getGson().fromJson(json, gsonTypes.<Path, String> getMapTypeToken());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
        Assert.assertTrue(result.get(Path.ofString("//is.yaks.tests/a")).equals("ABC"));
	}

	@Test
	public void BasicTest() {
		
		//set properties host, port, cachesize
		Properties options = new Properties();
		options.setProperty("host", "localhost");
		options.setProperty("port", "7887");
		options.setProperty("cacheSize", "1024");

		yaks = yaks.login(options);
		//creates an admin
		admin = yaks.admin();
		// create storage		
		String stid = "demo";
		options = new Properties();
		options.setProperty("selector","/is.yaks.test/**");
		admin.add_storage(stid, options, "Memory", yaks);
		
		//create workspace
			
		
		 // get access with id
        Workspace workspace = yaks.workspace(Path.ofString("/"));
        Assert.assertNotNull(workspace);
        
        String subid = workspace.subscribe(Selector.ofString("/is.yaks.tests/*"), obs);
		
        
		// put simple tuple
		boolean isPutOK = workspace.put(Path.ofString("/is.yaks.tests/a"), new Value("ABC", Encoding.STRING), 0);
//		Assert.assertTrue(isPutOK);
		
		// get object Value from key
		Map<Path, Value> values = workspace.get(Selector.ofString("/is.yaks.tests/a"));
		String strValue="";
		try {
			for (Map.Entry<Path, Value> entry : values.entrySet()) {
				strValue = new String(entry.getValue().getValue().getBytes(), "UTF-8");
			}
			values.forEach((k,v)->System.out.println("Item : " + k + " Value : " + v.getValue().toString()));
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println("ABC= " + strValue);
//		Assert.assertEquals("ABC", strValue);

		yaks.logout();
	}

//	@After
	public void stop() 
	{
		yaks.close();
	}

}