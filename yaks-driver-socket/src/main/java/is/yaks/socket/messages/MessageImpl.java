package is.yaks.socket.messages;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import is.yaks.Encoding;
import is.yaks.Message;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Value;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.utils.MessageCode;

public class MessageImpl implements Message
{
	
	private static MessageImpl instance = null;

	//1VLE max 64bit
	static VLEEncoder encoder;
	//vle length
	static int vle_length, vle_bytes;
	//1VLE max 64bit
	static int correlation_id;
	//8bit
	static int flags;
	//1bit
	static int flag_a;
	//1bit
	static int flag_s;
	//1bit
	static int flag_p;

	int quorum = 0;
	int wsid = 0;
	
	Path path;
	Value value;

	ByteBuffer data;
	Selector selector;
	Encoding encoding;
	MessageCode messageCode;
	
	Map<Path, Value> valuesList;
	Map<String, String> dataList;	
	Map<Path, Value> workspaceList;
	Map<String, String> propertiesList;

	public MessageImpl() {

		// 8bit (1byte) comprises A,S,P (0xFF = 0b11111111) 
		flags = 0;
		// 1bit. Initialized to false. 
		flag_a = 0;
		// 1bit. Initialized to false.
		flag_s = 0;
		// 1bit. Initialized to false.
		flag_p = 0;		
		//vle_bytes
		vle_bytes = 8;
		// vle length		
		vle_length = (64 * 1024);
		// messageCode
		messageCode = null;
		// Correction is VLEEncoder max 64bit
		correlation_id = Utils.generate_correlation_id();
		// VLE encoder
		encoder = VLEEncoder.getInstance();
		// dataList
		dataList = new HashMap<String, String>();
		// propertiesList
		propertiesList = new HashMap<String, String>();
		//worskpaceList
		workspaceList = new HashMap<Path, Value>();
		// valuesList
		valuesList =  new HashMap<Path, Value>();	
	}

	/**
	 * Get instance of the Message implementation
	 * @return MessageImpl
	 */
	public static MessageImpl getInstance() {
		if( instance == null ) 
		{
			instance = new MessageImpl();
		}
		return instance;
	}

	@Override
	public ByteBuffer write(SocketChannel sock, Message msg) 
	{
		
		ByteBuffer buf_vle =  ByteBuffer.allocate(vle_bytes);
		ByteBuffer buf_msg =  ByteBuffer.allocate(vle_length);
		try {
			
			buf_msg.put((byte) msg.getMessageCode().getValue());
			buf_msg.put((byte) flags);
			buf_msg.put(VLEEncoder.encode(msg.getCorrelationID()));
			if(!msg.getPropertiesList().isEmpty()) {
				buf_msg.put(Utils.porpertiesListToByteBuffer(msg.getPropertiesList()));
			}
			if(!msg.getWorkspaceList().isEmpty()) {
				buf_msg.put(Utils.workspaceListToByteBuffer(msg.getWorkspaceList()));
			}
			if (msg.getPath() != null) 
			{
				buf_msg.put((byte)msg.getPath().toString().length());
				buf_msg.put(msg.getPath().toString().getBytes());
			}
			if(msg.getSelector() != null) {
				buf_msg.put((byte)msg.getSelector().toString().length());
				buf_msg.put(msg.getSelector().toString().getBytes());
			}
			// adding the msg length
			buf_msg.flip();
			int msg_size = buf_msg.limit();
			buf_vle.put(VLEEncoder.encode(msg_size));
			
			if(sock.isConnected()) 
			{
				sock.write((ByteBuffer) buf_vle.flip());
				sock.write(buf_msg);
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}

		return buf_msg;
	}

	@Override
	public Message read(ByteBuffer buffer){
		
		Value value;
		String strKey = "", strValue = "";
		Message msg = new MessageImpl();

		buffer.flip();
		int msgCode = ((int)buffer.get() & 0xFF);
		switch (msgCode) {
			case 0xD0:  msg = new MessageFactory().getMessage(MessageCode.OK, null);
						break;
			case 0xD1:  msg = new MessageFactory().getMessage(MessageCode.VALUES, null);
						break;
			case 0xB2:	msg = new MessageFactory().getMessage(MessageCode.NOTIFY, null);
						break;
			case 0xE0:  msg = new MessageFactory().getMessage(MessageCode.ERROR, null);
						break;
			default: break;
		}
		
		propertiesList = new HashMap<String, String>();
		
		msg.setFlags((int) buffer.get()); 								// get the flags of the msg
		
		msg.setCorrelationId(VLEEncoder.read_correlation_id(buffer)); 	// get the correlation_id (vle)
		
		if ((msgCode == 0xD0) || (msgCode == 0xB2)) {
			if(msg.getFlags() == 1) {
				int length_properties = (int) buffer.get(); // get length_properties
				if(length_properties > 0) 					
				{
					byte[] key_bytes = new  byte[length_properties];
					buffer.get(key_bytes, 0, length_properties);
					try {
						strValue = new String(key_bytes, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					strKey = strValue.substring(0, strValue.indexOf("="));
					strValue = strValue.substring(strValue.indexOf("=")+1);
					propertiesList.put(strKey, strValue);
					msg.add_property(strKey, strValue);
				}
			}
		}
		if (msgCode == 0xD1){
			int valFormat = (buffer.get() & 0xFF); 	//read 0x81 = 129
			buffer.get();  							// reads out the 0x00
			if(valFormat > 0x80) {
				while(buffer.hasRemaining()) 
				{
					int length_key = (int) buffer.get();
					byte[] key_bytes = new  byte[length_key];
					buffer.get(key_bytes, 0, length_key);
					
					int val_encoding = (int) buffer.get();
					if (val_encoding == 0x01) {
						byte[] val_format = new byte[2]; // gets the 0x01 0x20
						buffer.get(val_format, 0, 2); 
					}
					int length_value  = (int) buffer.get();
					byte[] value_bytes =  new byte[length_value];
					buffer.get(value_bytes, 0, length_value);

					try {
						strKey = new String(key_bytes, "UTF-8");
						strValue = new String(value_bytes, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					value = new Value();
					value.setValue(strValue);
					value.setEncoding(Encoding.getEncoding(val_encoding));
					msg.add_value(Path.ofString(strKey), value);
				}
			}
		} 
		return msg;
	}


	@Override
	public MessageCode getMessageCode() 
	{
		return messageCode;
	}

	@Override
	public int getFlags() {
		return flags;
	}

	@Override
	public int getCorrelationID() {
		return correlation_id;
	}
	
	@Override
	public Map<Path, Value> getValuesList(){
		return valuesList;
	}
	
	@Override
	public Map<String, String> getPropertiesList() {
		return propertiesList;
	}
	
	@Override
	public Map<Path, Value> getWorkspaceList() {
		return workspaceList;
	}
	
	public void setCorrelationId(int corr_id) 
	{
		correlation_id = corr_id;
	}

	public void setFlag_a() 
	{
		flag_a = 1;
		flags = (int) (flags | 0x04);
	}

	public void setFlag_s() 
	{
		flag_s = 1;
		flags = (int) (flags | 0x02);
	}

	public void setFlag_p() 
	{
		flag_p = 1;
		flags = (int) (flags | 0x01);
	}

	public void setFlags(int flgs) {
		flags = flgs;
	}
	@Override
	public void add_value(Path path, Value val) {
		valuesList.put(path, val);
	}
	
	@Override
	public void add_property(String key, String value) {
		setFlag_p();
		propertiesList.put(key, value);
	}

	@Override
	public void add_workspace(Path p, Value v) {
		//setFlag_p();
		workspaceList.put(p, v);
	}
	
	@Override
	public void add_selector(Selector select) {
		selector = select;
	}
	
	public String pprint() 
	{
		String pretty = "\n############ YAKS FE SOCKET MESSAGE ###################" 
				+ "\n# CODE: "+ messageCode + "\n"
				+ "\n# CORR.ID:"+ correlation_id + "\n"
				+ "\n# LENGTH: "+ vle_length + "\n"
				+ "\n# FLAGS: RAW: " + (byte)flags + " | "
				+ "A:" + flag_a + " S:" + flag_s + " P:" + flag_p + "'.\n";

		if (flag_p==1) {
			pretty = pretty + "\n# HAS PROPERTIES\n# NUMBER OF PROPERTIES: "+ propertiesList.size();

			for(Map.Entry<String, String> entry : propertiesList.entrySet()) 
			{
				pretty = pretty + "\n#========\n# "
						+ " KEY: "+entry.getKey()  
						+ " VALUE: "+entry.getValue();
			}
		}
		pretty = pretty + "\n#========\nDATA:"+data.toString()
		+ "\n#######################################################";
		return pretty;
	}


	@Override
	public Path getPath() {
		return path;
	}
		
	@Override
	public void setPath(Path p) {
		this.path = p;
	}
	
	@Override
	public Selector getSelector() {
		return selector;
	}
	
	@Override
	public void setSelector(Selector s) {
		selector = s;
	}


	@Override
	public Value getValue() {
		return value;
	}

	@Override
	public void setValue(Value val) {
		value = val;
	}
}

class HeaderMessage extends MessageImpl {

	public static int P_FLAG = 0x01;
	public static int FLAGS_MASK = 0x01;

	public HeaderMessage(int correlation_id, Properties properties) {

	}	

	public static boolean has_flag(int h, int f) {
		return (h & f) != 0;
	}

	public static boolean has_properties() {
		return (flags & HeaderMessage.P_FLAG) != 0;
	}
}

class LoginMessage extends MessageImpl {

	public LoginMessage() {
		messageCode = MessageCode.LOGIN;
		flags = 0x0;
	}

	public LoginMessage(Properties properties) {
		messageCode = MessageCode.LOGIN;
		flags = 0x0;
		if(properties != null) {
			String username = (String)properties.get("username");
			String password = (String)properties.get("password");		
			if(username != null && !username.equals("None") && password!=null && !password.equals("None")) 
			{
				add_property("yaks.login", ""+username+":"+password+"");
			}		
		}
	}
}

class LogoutMessage extends MessageImpl {

	public LogoutMessage() {
		messageCode = MessageCode.LOGOUT;
	}
}

class WorkspaceMessage extends MessageImpl {
	
	public WorkspaceMessage() {
		
		messageCode = MessageCode.WORKSPACE;
	}
			
	public WorkspaceMessage(Path path) {
		
		messageCode = MessageCode.WORKSPACE;
		
		add_property("path", path.toString());

	}
}

class PutMessage extends MessageImpl {

	public PutMessage() {
		messageCode = MessageCode.PUT;
	}
	
	public PutMessage(Properties properties) {
		
		messageCode = MessageCode.PUT;
		quorum = 1;
		
	}
}

class GetMessage extends MessageImpl {

	public GetMessage() {
		messageCode = MessageCode.GET;
	}
}

class UpdateMessage extends MessageImpl {

	public UpdateMessage() {
		messageCode = MessageCode.UPDATE;
	}
}

class DeleteMessage extends MessageImpl {

	public DeleteMessage() {
		messageCode = MessageCode.DELETE;
	}
}

class SubscribeMessage extends MessageImpl {

	public SubscribeMessage() {
		messageCode = MessageCode.SUB;
	}
}

class UnsubscribeMessage extends MessageImpl {

	public UnsubscribeMessage() {
		messageCode = MessageCode.UNSUB;
	}
}

class NotifyMessage extends MessageImpl {

	public NotifyMessage() {
		messageCode = MessageCode.NOTIFY;
	}
}

class EvalMessage extends MessageImpl {

	public EvalMessage() {
		messageCode = MessageCode.EVAL;
	}
}

class RegisterEvalMessage extends MessageImpl {

	public RegisterEvalMessage() {
		messageCode = MessageCode.REG_EVAL;
	}
}

class UnregisterEvalMessage extends MessageImpl {

	public UnregisterEvalMessage() {
		messageCode = MessageCode.UNREG_EVAL;
	}
}

class ValuesMessage extends MessageImpl {

	public ValuesMessage() {
		messageCode = MessageCode.VALUES;
	}
}

class OkMessage extends MessageImpl {

	public OkMessage() {
		messageCode = MessageCode.OK;
	}
}

class ErrorMessage extends MessageImpl {

	public ErrorMessage() {
		messageCode = MessageCode.ERROR;
	}
}





