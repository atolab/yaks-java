package is.yaks.socket.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class MessageOpen implements Message {
	
	private byte[] body = { 0x00 };
	private ByteBuffer data;
	private byte[] raw_msg = { 0x00, 0x01, 0x02, 0x03, 0x04};
	//1VLE max 64bit
	private VLEEncoder encoder;
	//1VLE max 64bit
	private int correlaion_id;
	private int length;
	//1byte
	private int flags;
	//1bit
	private int flag_a;
	//1bit
	private int flag_s;
	//1bit
	private int flag_p;
	//vle length max 64 bits
	private int vle = 1024;
	private int encoding;
	private int message_code;
	private Map<String, String> dataList;	
	private Map<String, String> propertiesList;
	
	private String username;
	private String password;
	
	private boolean is_connected = false;

//	UTF8Encoding utf8 = new UTF8Encoding(false);
	
	public MessageOpen() {
		// TODO Auto-generated constructor stub
		super();
		this.encoder = VLEEncoder.getInstance();
		this.correlaion_id = Utils.generate_correlation_id();
	}


	public MessageOpen(String username, String password) {
		encoder = VLEEncoder.getInstance();
		this.propertiesList =  new HashMap<String, String>();		
		this.correlaion_id = Utils.generate_correlation_id();
		this.message_code = MessageCode.OPEN.getValue();
		if(!username.equals("None") && !password.equals("None")) 
		{
			add_property("yaks.login", "'"+username+":"+password+"'");
		}
		else 
		{
			add_property("yaks.login", "None:None");	
		}
		return;
	}

	@Override
	public ByteBuffer write() 
	{
		System.out.println("Inside MessageOpen::write() method.");

		ByteBuffer buffer =  ByteBuffer.allocate(vle);

		//set header		
		buffer.put((byte) 0x00); 						// keep space for the msg length
		buffer.put((byte) MessageCode.OPEN.getValue());
		buffer.put((byte) this.flags);
		buffer.put(encoder.encode(this.correlaion_id));
		
		// adding the msg length
		buffer.flip();
		int limit = buffer.limit();
		buffer.put(0, (byte) (limit-1));
        
		return buffer;
	}

	@Override
	public Message read(ByteBuffer buffer) {
		MessageOk msg_ok = null;
		
//		int limit =  buffer.limit();
//		int capacity = buffer.capacity();			
		
		vle = (byte) buffer.get(); 	// get the length of the response msg
		message_code = buffer.get();	// get the message_id
		
		// check if the returned msg code is OK
		if ((message_code & 0xFF) == 0xD0) {
			System.out.println("Got message OK");
			is_connected =  true;
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
			printHex(bb.array());
			correlaion_id = encoder.decode(bb.array()); // decode the correlation id
			msg_ok.setCorrelation_id(correlaion_id);
		}
		
		return msg_ok;
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
	
	public static void printHex(byte[] bytes) {
		for(int i = 0 ; i <  bytes.length; i++)
			System.out.print(Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ");
		System.out.println();
	}
}
