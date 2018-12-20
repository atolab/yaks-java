package is.yaks.socket.messages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import is.yaks.Encoding;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class MessagePut implements Message {
	
	private byte[] body = { 0x00 };
	private int data;
	private byte[] raw_msg = { 0x00, 0x01, 0x02, 0x03, 0x04};
	//1VLE max 64bit
	private VLEEncoder encoder;
	//1VLE max 64bit
	private int correlation_id;
	private int vle;
	//1byte
	private int flags;
	//1bit
	private int flag_a;
	//1bit
	private int flag_s;
	//1bit
	private int flag_p;

	//vle length max 64 bits
	private int vle_length = 1024;
	private Encoding encoding;
	private int message_code;
	private Map<String, String> dataList;	
	private Map<String, String> propertiesList;
	

	public MessagePut() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public MessagePut(String id, String key, String value) 
	{
		this.encoder = VLEEncoder.getInstance();

		this.dataList = new HashMap<String, String>();
		this.propertiesList = new HashMap<String, String>();
		
		
		this.message_code = MessageCode.PUT.getValue();
		this.encoding = Encoding.BYTE_BUFFER;
		this.correlation_id = Utils.generate_correlation_id();
		if(!id.equals("")) 
		{	
			add_property("is.yaks.access.id", id);
		}
		this.dataList.put(key, value);
		
	}
		
	@Override
	public ByteBuffer write() {
	
		System.out.println(">> MessagePut::write() method.");
		
		ByteBuffer buffer =  ByteBuffer.allocate(vle_length);

		//set header		
		buffer.put((byte) 0x00); 						 // keep space for the vle
		buffer.put((byte) MessageCode.PUT.getValue());   // 0xA0 PUT		
		buffer.put((byte) this.flags);        			 // i.e. 0x00
		buffer.put(encoder.encode(this.correlation_id)); // correlation_id in vle random 32 bits
		
		if (this.propertiesList.size() > 0) 
		{
			buffer.put(Utils.mapToByteBuffer(this.propertiesList));
		}
		if (this.dataList.size() > 0) 
		{
			buffer.put(Utils.mapDataToByteBuffer(this.dataList));
		}
				
		// adding the msg length
		buffer.flip();
		int limit = buffer.limit();
		buffer.put(0, (byte) (limit-1));
        
		return buffer;
	}
	
	public Message read(ByteBuffer[] arrBuffer) {
		MessageOk msg_ok = null;
		int i = 0;
		int curByte = 0;
		while(i < arrBuffer.length) {
			curByte = (int) arrBuffer[i].get();
			if((curByte & 0xFF) == MessageCode.NOTIFY.getValue()){
				System.out.println("Got message NOTIFY");
				int something = (int) arrBuffer[i].getInt(); // get something 4 bytes {00 00 01 30} idk what's is this.
			}
		}
		return null;
	}

	@Override
	public Message read(ByteBuffer buffer) 
	{
		
		MessageOk msg_ok = null;
		MessageNotify msg_notify = null;
		
		int limit = buffer.limit();
		
		message_code = (int) buffer.get();	// get the message id
		// check if the returned msg code is NOTIFY 0xB2
		if ((message_code & 0xFF) == MessageCode.NOTIFY.getValue()) 
		{
			msg_notify = new MessageNotify();
			System.out.println("Got message NOTIFY");
			
			int something = (int) buffer.getInt(); // get something 4 bytes {00 00 01 30} idk what's is this.
			
			data = (int) buffer.get(); // nr data 	
			if(data == 0x01) 					
			{
				int length_key = (int) buffer.get();
				byte[] key_bytes = new  byte[length_key];
				buffer.get(key_bytes, 0, length_key);
				
				int encoding = (int) buffer.get(); 
				int length_value  = (int) buffer.get();
				byte[] value_bytes =  new byte[length_value];
				buffer.get(value_bytes, 0, length_value);
				
				String strKey = "", strValue = "";
				try {
					strKey = new String(key_bytes, "UTF-8");
					strValue = new String(value_bytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				System.out.println("Decoded key: "+ strKey+ " decoded value:"+ strValue);
			}
			
			return msg_notify;
		}
		else if ((message_code & 0xFF) == MessageCode.OK.getValue()) 
		{
			msg_ok = new MessageOk();
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
			printHex(bb.array());
			this.correlation_id = encoder.decode(bb.array()); // decode the correlation id	
			
			return msg_ok;
		}
		return null;
	}
	
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
	
	public int getCorrelation_id() {
		return correlation_id;
	}

	public void setCorrelation_id(int correlation_id) {
		this.correlation_id = correlation_id;
	}
	
	public static void printHex(byte[] bytes) {
		for(int i = 0 ; i <  bytes.length; i++)
			System.out.print(Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ");
		System.out.println();
	}
}
