package is.yaks.socket.messages;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import is.yaks.Access;
import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.Storage;
import is.yaks.Value;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socketfe.EntityType;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class MessageImpl implements Message
{

	
	
	private static MessageImpl instance = null;

	//1VLE max 64bit
	static VLEEncoder encoder;
	//vle length
	static int vle_length;
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
	Access access;
	Storage storage;
	ByteBuffer data;
	Selector selector;
	Encoding encoding;
	EntityType entityType;
	MessageCode messageCode;
	
	Map<String, String> dataList;	
	
	Map<Path, Value> valuesList;
	
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
		// vle length
		vle_length = 1024;
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
		System.out.println("Message.write()");
		
		ByteBuffer bufVle =  ByteBuffer.allocate(1);
		ByteBuffer buffer =  ByteBuffer.allocate(vle_length);
		try {
			
			buffer.put((byte) msg.getMessageCode().getValue());
			buffer.put((byte) flags);
			buffer.put(VLEEncoder.encode(msg.getCorrelationID()));
			if(!msg.getPropertiesList().isEmpty()) {
				buffer.put(Utils.porpertiesListToByteBuffer(msg.getPropertiesList()));
			}
			if(!msg.getWorkspaceList().isEmpty()) {
				buffer.put(Utils.workspaceListToByteBuffer(msg.getWorkspaceList()));
			}
			if (msg.getPath() != null) 
			{
				buffer.put((byte)msg.getPath().toString().length());
				buffer.put(msg.getPath().toString().getBytes());
			}
			if(msg.getSelector() != null) {
				buffer.put((byte)msg.getSelector().toString().length());
				buffer.put(msg.getSelector().toString().getBytes());
			}
			// adding the msg length
			buffer.flip();
			int limit = buffer.limit();
			bufVle.put(0, (byte) (limit));

			if(sock.isConnected()) 
			{
				sock.write(bufVle);
				sock.write(buffer);
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}

		return buffer;
	}

	@Override
	public Message read(SocketChannel sock, ByteBuffer buffer){
		
		System.out.println("Message.read()");
		
		Message msg = null;

		try {
			int msgSize = VLEEncoder.decode(buffer.array()); // decode the vle length
			
			ByteBuffer bytesData = ByteBuffer.allocate(msgSize);
			sock.read(bytesData.order(ByteOrder.BIG_ENDIAN));
			bytesData.flip();  

			int msgCode = bytesData.get();

			if ((msgCode & 0xFF) == 0xD0) {
				System.out.println("Received OK msg 0xD0!");
				msg = new MessageFactory().getMessage(MessageCode.OK, null);
				propertiesList = new HashMap<String, String>();
				int flags = (int) bytesData.get(); //get the flags from the msg
				msg.setFlags(flags);
				// read the vle correlation_id one byte at the time
				
				byte curByte = bytesData.get();
				ByteBuffer bb = ByteBuffer.allocate(VLEEncoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				while((curByte & VLEEncoder.MORE_BYTES_FLAG) !=0) {
					bb.put((byte) (curByte));
					curByte = bytesData.get();
				}			
				bb.put(curByte);
				printHex(bb.array());
				int corr_id = VLEEncoder.decode(bb.array()); // decode the correlation id
				msg.setCorrelationId(corr_id);
				
				if(msg.getFlags() == 1) {
					
					int length_properties = (int) bytesData.get(); // get length_properties
							
					if(length_properties > 0x00) 					
					{
						byte[] value_bytes = new  byte[length_properties];
						bytesData.get(value_bytes, 0, length_properties);
						
						String strKey = "", strValue = "";
						try {
							strValue = new String(value_bytes, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						strKey = strValue.substring(0, strValue.indexOf("="));
						strValue = strValue.substring(strValue.indexOf("=")+1);
						System.out.println("Decoded key: "+ strKey+ " decoded-value:"+ strValue);
						propertiesList.put(strKey, strValue);
						msg.add_property(strKey, strValue);
						
					}
				}
				
			} else if ((msgCode & 0xFF) == 0xD1) {
				System.out.println("Received VALUES msg 0xD1!");
				msg = new MessageFactory().getMessage(MessageCode.VALUES, null);
				Value value; 
				
				int flags = (int) bytesData.get(); //get the flags from the msg
				msg.setFlags(flags);
				// read the vle correlation_id one byte at the time
				byte curByte = bytesData.get();
				ByteBuffer bb = ByteBuffer.allocate(VLEEncoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				while((curByte & VLEEncoder.MORE_BYTES_FLAG) !=0) {
					bb.put((byte) (curByte));
					curByte = bytesData.get();
				}			
				bb.put(curByte);
				printHex(bb.array());
				int corr_id = VLEEncoder.decode(bb.array()); // decode the correlation id
				msg.setCorrelationId(corr_id);
				
				
				int valFormat = (bytesData.get() & 0xFF); //read 0x81 = 129
				bytesData.get();  // reads out the 0x00
				
				if(valFormat > 0x80) {
					
					while(bytesData.hasRemaining()) 
					{
						int length_key = (int) bytesData.get();
						byte[] key_bytes = new  byte[length_key];
						bytesData.get(key_bytes, 0, length_key);
						
						int val_encoding = (int) bytesData.get();
						if (val_encoding == 0x01) {
							byte[] val_format = new byte[2]; // gets the 0x01 0x20
							bytesData.get(val_format, 0, 2); 
						}
						int length_value  = (int) bytesData.get();
						byte[] value_bytes =  new byte[length_value];
						bytesData.get(value_bytes, 0, length_value);
						String strKey = "", strValue = "";
						try {
							strKey = new String(key_bytes, "UTF-8");
							strValue = new String(value_bytes, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						System.out.println("Decoded key: "+ strKey+ " decoded-value: "+ strValue + " Encoding: "+val_encoding);
						value = new Value();
						value.setValue(strValue);
						value.setEncoding(Encoding.getEncoding(val_encoding));
						msg.add_value(Path.ofString(strKey), value);
					}
				}
				
				
			} else if ((msgCode & 0xFF) == 0xB2) {
				System.out.println("Received NOTIFY msg 0xB2!");
				msg = new MessageFactory().getMessage(MessageCode.NOTIFY, null);
				propertiesList = new HashMap<String, String>();
				int flags = (int) bytesData.get(); //get the flags from the msg
				msg.setFlags(flags);
				// read the vle correlation_id one byte at the time
				byte curByte = bytesData.get();
				ByteBuffer bb = ByteBuffer.allocate(VLEEncoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				while((curByte & VLEEncoder.MORE_BYTES_FLAG) !=0) {
					bb.put((byte) (curByte));
					curByte = bytesData.get();
				}			
				bb.put(curByte);
				printHex(bb.array());
				int corr_id = VLEEncoder.decode(bb.array()); // decode the correlation id
				msg.setCorrelationId(corr_id);
				
				if(msg.getFlags() == 1) {
					
					int length_properties = (int) bytesData.get(); // get length_properties
					if(length_properties > 0x00) 					
					{
						byte[] key_bytes = new  byte[length_properties];
						bytesData.get(key_bytes, 0, length_properties);
						
						String strKey = "", strValue = "";
						try {
							strKey = new String(key_bytes, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						strKey = strValue.substring(0, strValue.indexOf("="));
						strValue = strValue.substring(strValue.indexOf("=")+1);
						System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
						propertiesList.put(strKey, strValue);
						msg.add_property(strKey, strValue);
					}
				} 
				
			} else if ((msgCode & 0xFF) == 0xE0) {
				System.out.println("Received ERROR msg 0xE0!");
				msg = new MessageFactory().getMessage(MessageCode.ERROR, null);
			}

		} catch (IOException e) {
			e.printStackTrace();
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
	public  Map<Path, Value> getValuesList(){
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

	
	
	public static void printHex(byte[] bytes) {
		for(int i = 0 ; i <  bytes.length; i++)
			System.out.print(Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ");
		System.out.println();
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





