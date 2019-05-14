package is.yaks.socket.types;

import java.util.Map;

import is.yaks.Path;
import is.yaks.YSelector;
import is.yaks.Value;

public interface Message {

    public static String WSID = "wsid";
    public static String AUTO = "auto";
    public static String SUBID = "subid";

    /**
     * Returns the message code
     * 
     * @return MessageCode
     */
    public MessageCode getMessageCode();

    /**
     * Returns the set of tuples *<path,value>* available in YAKS
     * 
     * @return
     */
    public Map<Path, Value> getValuesList();

    /**
     * Returns the message properties
     * 
     * @return properties
     */
    public Map<String, String> getPropertiesList();

    /**
     * Returns the list workspaces
     * 
     * @return list
     */
    public Map<Path, Value> getWorkspaceList();

    /**
     * Returns the flags of the message
     * 
     * @return flags
     */
    public int getFlags();

    /**
     * Returns the correlation id
     * 
     * @return correlation id
     */
    public long getCorrelationID();

    /**
     * Set the flags of the message
     * 
     * @param flags
     */
    public void setFlags(int flags);

    /**
     * Set the correlation id of the message
     * 
     * @param corr_id
     */
    public void setCorrelationId(long corr_id);

    /**
     * Set the message selector
     * 
     * @param selector
     */
    public void add_selector(YSelector selector);

    /**
     * Add a message's properties
     * 
     * @param key
     * @param value
     */
    public void add_property(String key, String value);

    /**
     * Add a set of tuples <path, value> to a message
     * 
     * @param path
     * @param value
     */
    public void add_value(Path path, Value value);

    /**
     * Add a workspace
     * 
     * @param path
     * @param value
     */
    public void add_workspace(Path p, Value v);

    /**
     * Get the path
     * 
     * @return path
     */
    public Path getPath();

    /**
     * Set the path
     * 
     * @param path
     */
    public void setPath(Path path);

    /**
     * Get the selector
     * 
     * @return selector
     */
    public YSelector getSelector();

    /**
     * Set the selector
     * 
     * @param selector
     */
    public void setSelector(YSelector selector);

    /**
     * Get the Value
     * 
     * @return Value
     */
    public Value getValue();

    /**
     * Set the value of message
     * 
     * @param value
     */
    public void setValue(Value value);

    /**
     * Get the subscriber id
     * 
     * @param string
     */
    public String getSubid();

    /**
     * Set the subscriber id
     * 
     * @param string
     */
    public void setSubid(String string);

}

interface Header {

    static int P_FLAG = 0x01;
    static int FLAG_MASK = 0x01;

    boolean has_flag(int h, int f);

    boolean has_properties();

}
