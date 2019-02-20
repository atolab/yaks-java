package is.yaks;

import java.nio.channels.SocketChannel;

import is.yaks.socketfe.Message;

public interface YaksRuntime {
	
	public static int DEFAULT_TIMEOUT = 5;
	
	
	/**
	 * 
	 */
	public void close();
	
	public boolean post_message(SocketChannel sock, Message msg);
	
	public void add_listener();
	
	public void remove_listener();
	
	public void add_eval_callback();
	
	public void remove_eval_callback();
	
	public void notify_listeners();
	
	public void execute_eval();
	
	public void handle_reply();
	
	public void handle_unexpected_message();
	
	public void run();

}
