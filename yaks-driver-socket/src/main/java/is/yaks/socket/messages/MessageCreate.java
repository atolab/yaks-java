package is.yaks.socket.messages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import is.yaks.Access;
import is.yaks.Path;
import is.yaks.Storage;
import is.yaks.socket.AccessImpl;
import is.yaks.socket.StorageImpl;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socketfe.EntityType;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;
import is.yaks.socketfe.MessageFlags;

public class MessageCreate implements Message {


	//1VLE max 64bit
	private VLEEncoder encoder;
	//1VLE max 64bit
	private int correlation_id;
	//1byte
	private int flags;
	//1bit
	private int flag_a;
	//1bit
	private int flag_s;
	//1bit
	private int flag_p;

	private int vle = 1024;
	private int encoding;
	private int message_code;
	private Map<String, String> dataList;	
	private Map<String, String> propertiesList;
	private Path path;	
	private Access access;
	private Storage storage;
	private boolean is_connected = false;
	
	public MessageCreate() {
		super();
		encoder = VLEEncoder.getInstance();
	}

	public MessageCreate(EntityType type, Path path, String id, int cache_size, String config, boolean complete) 
	{
		encoder = VLEEncoder.getInstance();
		this.propertiesList =  new HashMap<String, String>();
		JSONObject json = new JSONObject();
		this.message_code = MessageCode.CREATE.getValue();
		this.correlation_id = Utils.generate_correlation_id();
		this.path = path;
		
		if(type.equals(EntityType.ACCESS)) 
		{
			setFlag_a();			
			if(!id.equals("None") && !id.equals(""))
			{
				add_property("is.yaks.access.alias", id);
			}
			if(cache_size != 0)
			{
				add_property("is.yaks.access.cachesize", String.valueOf(cache_size));
			} 
		} 
		else if (type.equals(EntityType.STORAGE)) 
		{
			setFlag_s();
			if(!id.equals("None") && !id.equals("")) 
			{
				add_property("is.yaks.storage.alias", id);
			}
			if(!config.equals("None") && !config.equals("")) 
			{
				json = (JSONObject) JSONValue.parse(config);
				add_property("is.yaks.storage.config", json.toString());
			}
			if(complete) {			
				add_property("is.yaks.storage.complete", "true");
			}
		}
	}

	@Override
	public ByteBuffer write() {
		
		System.out.println("Inside MessageCreate::write() method.");
		ByteBuffer buffer =  ByteBuffer.allocate(vle);
		//set header		
		buffer.put((byte) 0x00); 							// keep space for the length
		buffer.put((byte) MessageCode.CREATE.getValue()); 	// 0x02 CREATE		
		buffer.put((byte) this.flags);        				// 0x02 A=1 & P=1
		buffer.put(encoder.encode(this.correlation_id)); 	// correlation_id int vle random 32 bits 
		
		if (this.propertiesList.size() > 0) 
		{
			buffer.put(Utils.mapToByteBuffer(this.propertiesList));
		}
		buffer.put((byte)this.path.toString().length());
		buffer.put((this.path.toString()).getBytes());
		
		// adding the msg length
		buffer.flip();
		int limit = buffer.limit();
		buffer.put(0, (byte) (limit-1));
		return buffer;
	}

	public Storage createStorage(ByteBuffer buffer) 
	{
		String strKey = "", strValue = "";

		//vle = (byte) buffer.get(); 		// get the length of the response msg
		message_code = buffer.get();	// get the message_id

		// check if the returned msg code is OK
		if ((message_code & 0xFF) == MessageCode.OK.getValue()) {
			System.out.println("Got message OK");
			flags = (int) buffer.get(); //get the flags from the msg
			// read the vle correlation_id one byte at the time
			byte curByte = buffer.get();
			ByteBuffer bb = ByteBuffer.allocate(encoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
			while((curByte & encoder.MORE_BYTES_FLAG) !=0) {
				bb.put((byte) (curByte));
				curByte = buffer.get();
			}			
			bb.put(curByte);
			Utils.printHex(bb.array());
			correlation_id = encoder.decode(bb.array()); // decode the correlation id			

			if(flags == MessageFlags.PROPERTY.getValue()) 
			{
				int encoding = (int) buffer.get();
				int length_key = (int) buffer.get();
				byte[] key_bytes = new  byte[length_key];
				buffer.get(key_bytes, 0, length_key);

				int length_value  = (int) buffer.get();
				byte[] value_bytes =  new byte[length_value];
				buffer.get(value_bytes, 0, length_value);

				try {
					strKey = new String(key_bytes, "UTF-8");
					strValue = new String(value_bytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				//				System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
				if(strKey.equals("is.yaks.storage.id")) {
					storage = new StorageImpl(strValue);
				} 
			}
		}
		return storage;
	}
	
	
	public Access createAccess(ByteBuffer buffer) 
	{
		String strKey = "", strValue = "";
//		vle = (byte) buffer.get(); 		// get the length of the response msg
		message_code = buffer.get();	// get the message_id

		// check if the returned msg code is OK
		if ((message_code & 0xFF) == MessageCode.OK.getValue()) {
			System.out.println("Got message OK");
			flags = (int) buffer.get(); //get the flags from the msg

			// read the vle correlation_id one byte at the time
			byte curByte = buffer.get();
			ByteBuffer bb = ByteBuffer.allocate(encoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
			while((curByte & encoder.MORE_BYTES_FLAG) !=0) {
				bb.put((byte) (curByte));
				curByte = buffer.get();
			}			
			bb.put(curByte);
			Utils.printHex(bb.array());
			correlation_id = encoder.decode(bb.array()); // decode the correlation id			

			if(flags == MessageFlags.PROPERTY.getValue()) 
			{
				int encoding = (int) buffer.get();
				int length_key = (int) buffer.get();
				byte[] key_bytes = new  byte[length_key];
				buffer.get(key_bytes, 0, length_key);

				int length_value  = (int) buffer.get();
				byte[] value_bytes =  new byte[length_value];
				buffer.get(value_bytes, 0, length_value);

				try {
					strKey = new String(key_bytes, "UTF-8");
					strValue = new String(value_bytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				//				System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
				if(strKey.equals("is.yaks.access.id")) {
					access = new AccessImpl(strValue, Path.ofString("//fos"), Long.parseLong("1024"));
				}
			}
		}
		return access;
	}
	
	@Override
	public Message read(ByteBuffer buffer) {
		
		MessageOk msg_ok = null;
		
//		int limit =  buffer.limit();
//		int capacity = buffer.capacity();			
		
		vle = (byte) buffer.get(); 		// get the length of the response msg
		message_code = buffer.get();	// get the message_id
		
		// check if the returned msg code is OK
		if ((message_code & 0xFF) == MessageCode.OK.getValue()) {
			System.out.println("Got message OK");
			msg_ok = new MessageOk();
			flags = (int) buffer.get(); //get the flags from the msg
			msg_ok.setFlags(flags);
			
			// read the vle correlation_id one byte at the time
			byte curByte = buffer.get();
			ByteBuffer bb = ByteBuffer.allocate(encoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
			while((curByte & encoder.MORE_BYTES_FLAG) !=0) {
				bb.put((byte) (curByte));
				curByte = buffer.get();
			}			
			bb.put(curByte);
			Utils.printHex(bb.array());
			correlation_id = encoder.decode(bb.array()); // decode the correlation id			
			msg_ok.setCorrelation_id(correlation_id);
			
			if(flags == MessageFlags.PROPERTY.getValue()) 
			{
				int encoding = (int) buffer.get();
				int length_key = (int) buffer.get();
				byte[] key_bytes = new  byte[length_key];
				buffer.get(key_bytes, 0, length_key);
				
				int length_value  = (int) buffer.get();
				byte[] value_bytes =  new byte[length_value];
				buffer.get(value_bytes, 0, length_value);
				
				
				String strKey = "", strValue = "";
				try {
					strKey = new String(key_bytes, "UTF-8");
					strValue = new String(value_bytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
				if(strKey.equals("is.yaks.storage.id")) {
					storage = new StorageImpl(strValue);
				} else if(strKey.equals("is.yaks.access.id")) {
					access = new AccessImpl(strValue, Path.ofString("//fos"), Long.parseLong("1024"));
				}
			}
		}
		return msg_ok;
	}
	
	
	
	
	/*
	 vle = (byte) buffer.get();   //get the vle
			msg_code = buffer.get();	//get the message id
			
			// check that the returned msg code is OK
			if ((msg_code & 0xFF) == MessageCode.OK.getValue()) {
				System.out.println("Got message Sub-OK");
				msgOK = new MessageOk();
				flags = (int) buffer.get(); //get the flags from the msg
				msgOK.setFlags(flags);
				// read the vle correlation_id one byte at the time
				byte curByte = buffer.get();
				ByteBuffer bb = ByteBuffer.allocate(encoder.MAX_BYTES).order(ByteOrder.BIG_ENDIAN);
				while((curByte & encoder.MORE_BYTES_FLAG) !=0) {
					bb.put((byte) (curByte));
					curByte = buffer.get();
				}			
				bb.put(curByte);
				printHex(bb.array());
				corr_id = encoder.decode(bb.array()); // decode the correlation id			
				msgOK.setCorrelation_id(corr_id);
				
				if(flags == MessageFlags.PROPERTY.getValue()) 
				{
					int nro_properties = (int) buffer.get();
					
					int length_key = (int) buffer.get();
					byte[] key_bytes = new  byte[length_key];
					buffer.get(key_bytes, 0, length_key);
					
					int length_value  = (int) buffer.get();
					byte[] value_bytes =  new byte[length_value];
					buffer.get(value_bytes, 0, length_value);
					
					String strKey = new String(key_bytes, "UTF-8");
					String strValue = new String(value_bytes, "UTF-8");
					System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
					if(strKey.equals("is.yaks.access.id")) {
						access = new AccessImpl(strValue, Path.ofString("//fos"), Long.parseLong("1024"));
					}
				}
			}
	 * */
	
	
	
	
	public void setFlag_a() 
	{
		this.flag_a = 1;
		this.flags = (int) (this.flags | 0x04);
	}
	
	public void setFlag_s() 
	{
		this.flag_s = 1;
		this.flags = (int) (this.flags | 0x02);
	}
	
	public void setFlag_p() 
	{
		this.flag_p = 1;
		this.flags = (int) (this.flags | 0x01);
	}
	
	public void add_property(String key, String value) {
		setFlag_p();
		this.propertiesList.put(key, value);
	}

}
