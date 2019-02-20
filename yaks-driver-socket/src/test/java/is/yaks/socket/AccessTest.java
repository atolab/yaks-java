package is.yaks.socket;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import is.yaks.Access;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Storage;
import is.yaks.Value;
import is.yaks.Yaks;
import is.yaks.socket.utils.GsonTypeToken;
import is.yaks.socket.utils.YaksConfiguration;
import junit.framework.Assert;

public class AccessTest {

	private Yaks yaks;

	public static final Logger LOG = LoggerFactory.getLogger(AccessTest.class);
    private GsonTypeToken gsonTypes = GsonTypeToken.getInstance();

	@Before
	public void init() {
		String[] args = { "http://localhost:7887" };
		yaks = Yaks.getInstance("is.yaks.socket.YaksImpl", AccessTest.class.getClassLoader(), args);
		Assert.assertTrue(yaks instanceof YaksImpl);
	}

	//@Test
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

		// create storage
		Storage storage = yaks.createStorage(Path.ofString("/is.yaks.test"), options);
				
		// create access
		Access access1 = yaks.createAccess("access-1", Path.ofString("/is.yaks.tests"), Integer.parseInt(options.getProperty("cacheSize")), Encoding.JSON);
		Assert.assertNotNull(access1);
		
		// create subscription
		Long sid = access1.subscribe(Selector.ofString("/is.yaks.tests/*"));
		Assert.assertTrue(sid >= 0);
		
		// put simple tuple
		boolean isPutOK = access1.put(Selector.ofString("/is.yaks.tests/a"), "ABC");
		Assert.assertTrue(isPutOK);
		
		// get object Value from key
		Value value = access1.get(Selector.ofString("/is.yaks.tests/a"));
		String strValue="";
		try {
			strValue = new String(value.getValue().array(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Assert.assertEquals("ABC", strValue);

		// put an object Value
		boolean isPutOK2 = access1.put(Selector.ofString("/is.yaks.tests/value"), value);
		Assert.assertTrue(isPutOK2);
		
		// create access 2
		Access access2 = yaks.createAccess("access-2", Path.ofString("/is.yaks.tests-2"), Integer.parseInt(options.getProperty("cacheSize")), Encoding.JSON);
		Assert.assertNotNull(access2);

		// create access 3
		Access access3 = yaks.createAccess("access-3", Path.ofString("/is.yaks.tests-3"), Integer.parseInt(options.getProperty("cacheSize")), Encoding.JSON);
		Assert.assertNotNull(access3);
		
		boolean isRemoveOK = access3.remove(Path.ofString("//is.yaks.tests-3"));
		Assert.assertTrue(isRemoveOK);

		// unsubscribe access with subscription id
		boolean isUnsubscribeOK = access1.unsubscribe(sid.toString());
		Assert.assertTrue(isUnsubscribeOK);

		// dispose all the access
		access1.dispose();
		access2.dispose();
		access3.dispose();
		
		// dispose storage		
		storage.dispose();
	}

	@After
	public void stop() 
	{
		yaks.close();
	}

}