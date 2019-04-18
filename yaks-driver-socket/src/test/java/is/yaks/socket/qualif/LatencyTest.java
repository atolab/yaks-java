package is.yaks.socket.qualif;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import is.yaks.Path;
import is.yaks.Value;
import is.yaks.Workspace;
import is.yaks.Yaks;
import is.yaks.socket.YaksImpl;

public class LatencyTest {
	
	public static int DEFAUL_PORT = 7887;
    
	private static Yaks yaks;
    private static Workspace ws;


    public void init() throws InterruptedException, ExecutionException {
		yaks =  YaksImpl.getInstance();
		
    	Properties properties = new Properties();
		properties.setProperty("host", "localhost");
		properties.setProperty("port", "7887");
		properties.setProperty("cacheSize", "1024");

        if (yaks != null) {
        	//login to yaks api
    		yaks = yaks.login(properties);
    		//creates workspace
	        ws = yaks.workspace(Path.ofString("/"));
        }
    }

    public void put_n(long n, Workspace ws, Path path, Value val) {
    	if(n > 1) {
    		ws.put(path, val, 0);
    		put_n(n-1, ws, path, val);
    	} 
    	else 
    		ws.put(path, val, 0);
    }
    
    private static String create_data(int size) {
    	char[] chars = new char[size];
    	Arrays.fill(chars, 'a');
    	return new String(chars);
    }

    public void runPut(long samples, int size) 
    {
    	Path path = Path.ofString("test/thr/put");
    	Value val = new Value(create_data(size));
    	long start = System.currentTimeMillis();
    	put_n(samples, ws, path, val);
    	long stop = System.currentTimeMillis();
    	long delta = stop - start;
    	System.out.println("Throughput: "+ ((double)samples / delta)*1000 +" msg/sec \n");
    }	
    
	public static void main(String[] args) {
		LatencyTest lt = new LatencyTest();
		try {		
			
			lt.init();
			long start = System.currentTimeMillis();
			lt.runPut(10000, 1024);
			long stop = System.currentTimeMillis();
			long delta = stop - start;
			System.out.println("Latency: 10000 puts of msg-size = 1024bytes, time: "+ delta +" ms \n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
