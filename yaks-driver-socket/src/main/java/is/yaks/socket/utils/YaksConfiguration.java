package is.yaks.socket.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;

import com.google.gson.Gson;

public class YaksConfiguration {

    private String yaksUrl;
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private SocketChannel socketChannel;
	private JSONObject jsonObj;
	private Gson gson;
	private BufferedReader input = null;

    // first load at the call of YaksConfiguration.getInstance()
    // work in multithread env
    public static YaksConfiguration getInstance() {
        return YaksConfigurationHolder.instance;
    }

    private static class YaksConfigurationHolder {
        private final static YaksConfiguration instance = new YaksConfiguration();
    }

    private YaksConfiguration() {
        
    	//set properties host, port, cachesize
		Properties options = new Properties();
		options.setProperty("host", "localhost");
		options.setProperty("port", "7887");
		options.setProperty("cacheSize", "1024");
		
		try 
		{		
			InetSocketAddress addr = new InetSocketAddress(options.getProperty("host"), Integer.parseInt(options.getProperty("port")));
			Selector selector = Selector.open();			
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(addr);
			socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			
	        jsonObj = new JSONObject();        
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public SocketChannel getChannel() {
		return socketChannel;
	}

	public void setChannel(SocketChannel channel) {
		this.socketChannel = channel;
	}

	public JSONObject getJsonObj() {
		return jsonObj;
	}

	public void setJsonObj(JSONObject jsonObj) {
		this.jsonObj = jsonObj;
	}

	public String getYaksUrl() {
        return this.yaksUrl;
    }

    public void setYaksUrl(String yaksUrl) {
        this.yaksUrl = yaksUrl;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    public Gson getGson() {
        return gson;
    }

}