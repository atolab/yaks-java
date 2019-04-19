package is.yaks;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.utils.MessageCode;

public interface Message 
{
	
	/**
	This part is in enum MessageCode
	public static int LOGIN 	= 0x01;
	public static int LOGOUT 	= 0x02;
	public static int WORKSPACE = 0X03;
	
	public static int PUT 	  = 0xA0;
	public static int UPDATE  = 0xA1;
	public static int GET 	  = 0xA2;
	public static int DELETE  = 0xA3;
	
	public static int SUB 	  = 0xB0;
	public static int UNSUB   = 0xB1;
	public static int NOTIFY  = 0xB2;	
	
	public static int REG_EVAL 	 = 0xC0;
	public static int UNREG_EVAL = 0xC1;
	public static int EVAL  	 = 0xC2;

	public static int OK 	 = 0xD0;
	public static int VALUES = 0xD1;

	public static int ERROR	 = 0xE0;
	*/
	
	public static String WSID	 = "wsid";
	public static String AUTO	 = "auto";
	public static String SUBID	 = "subid";
	
	
	/**
	 * Writes a Message into a SocketChannel 
	 * 
	 * @return ByteBuffer
	 */
	public ByteBuffer write(SocketChannel sock, Message msg);	

	/**
	 * Read data from the buffer into a Message
	 * 
	 * @param buffer
	 * @return Message
	 */
	public Message read(ByteBuffer buffer);
	
	/**
	 * Returns the message code
	 * @return MessageCode
	 */
	public MessageCode getMessageCode();
	
	/**
	 * Returns the set of tuples  *<path,value>* available in YAKS
	 * @return 
	 */
	public Map<Path, Value> getValuesList();
	
	/**
	 * Returns the message properties
	 * @return properties
	 */
	public Map<String, String> getPropertiesList();
	
	/**
	 * Returns the list workspaces 
	 * @return list
	 */
	public Map<Path, Value> getWorkspaceList();
	
	/**
	 * Returns the flags of the message
	 * @return flags
	 */
	public int getFlags();
	
	/**
	 * Returns the correlation id
	 * @return correlation id
	 */
	public int getCorrelationID();

	/**
	 * Set the flags of the message
	 * @param flags
	 */
	public void setFlags(int flags);

	/**
	 * Set the correlation id of the message
	 * @param corr_id
	 */
	public void setCorrelationId(int corr_id);
	
	/**
	 * Set the message selector
	 * @param selector
	 */
	public void add_selector(Selector selector);
	
	/**
	 * Add a message's properties
	 * @param key
	 * @param value
	 */
	public void add_property(String key, String value); 
	
	/**
	 * Add a set of tuples <path, value> to a message
	 * @param path
	 * @param value
	 */
	public void add_value(Path path, Value value);
	
	/**
	 * Add a workspace
	 * @param path
	 * @param value
	 */
	public void add_workspace(Path p, Value v);
	
	/**
	 * Get the path
	 * @return path
	 */
	public Path getPath();
		
	/**
	 * Set the path
	 * @param path
	 */
	public void setPath(Path path);

	/**
	 * Get the selector
	 * @return selector
	 */
	public Selector getSelector();

	/**
	 * Set the selector
	 * @param selector
	 */
	public void setSelector(Selector selector);
	/**
	 * Get the Value
	 * @return Value
	 */
	public Value getValue();
	
	/**
	 * Set the value of message
	 * @param value
	 */
	public void setValue(Value value);
	

}

interface Header{
	
	static int P_FLAG 	 = 0x01;
	static int FLAG_MASK = 0x01;
	
	
	boolean has_flag();
	
	boolean has_properties();
	
	
}


