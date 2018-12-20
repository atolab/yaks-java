package is.yaks.socket.messages;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import is.yaks.Path;
import is.yaks.Selector;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.Encoding;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class MessageUnsub implements Message {
	
	private byte[] body = { 0x00 };
	private ByteBuffer data;
	private byte[] raw_msg = { 0x00, 0x01, 0x02, 0x03, 0x04};
	//1VLE max 64bit
	private VLEEncoder encoder;
	//1VLE max 64bit
	private int correlation_id;
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
	private int vle_length= 1024;
	private Encoding encoding;
	private int message_code;
	private Map<String, String> dataList;	
	private Map<String, String> propertiesList;
	private Map<String, String> subscriptions;
	
	private Path path;
	private Selector selector;
	

	public MessageUnsub() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public MessageUnsub(String access_id, String subscription_id) {
		
		this.encoder = VLEEncoder.getInstance();
		this.propertiesList =  new HashMap<String, String>();
		this.data = ByteBuffer.allocate(subscription_id.length());
		
		this.message_code = MessageCode.UNSUB.getValue();
		
		this.encoding = Encoding.BYTE_BUFFER;
		this.correlation_id = Utils.generate_correlation_id();
		if(!access_id.equals("")) 
		{	
			add_property("is.yaks.access.id", access_id);
		}
		if(!subscription_id.equals("")) 
		{
			this.data.put(subscription_id.getBytes());
		}
	}

	@Override
	public ByteBuffer write() {
		System.out.println("Inside MessageSub::write() method.");
		
		ByteBuffer buffer =  ByteBuffer.allocate(vle_length);

		//set header		
		buffer.put((byte) 0x00); 						 // keep space for the vle
		buffer.put((byte) MessageCode.UNSUB.getValue()); // 0xB0 SUB		
		buffer.put((byte) this.flags);        			 // i.e. 0x02
		buffer.put(encoder.encode(this.correlation_id)); // correlation_id in vle random 32 bits
		
		if (this.propertiesList.size() > 0) 
		{
			buffer.put(Utils.mapToByteBuffer(this.propertiesList));
		}
		if(data != null) 
		{
			// adding the data i.e. the subscription_id
			data.flip();
			buffer.put((byte)this.data.limit());
			buffer.put(this.data.get());
		}
				
		// adding the vle length of the msg
		buffer.flip();
		int limit = buffer.limit();
		buffer.put(0, (byte) (limit-1));
        
		return buffer;
	}

	@Override
	public Message read(ByteBuffer buffer) {
		MessageOk msg_ok = null;
		int limit = buffer.limit();
		
		message_code = buffer.get();	//get the message id
		// check if the returned msg code is OK
		if ((message_code & 0xFF) == MessageCode.OK.getValue()) {
			
			msg_ok =  new MessageOk();
			System.out.println("Got message OK");
			subscriptions = new HashMap<String, String>();
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
		}
		return msg_ok;
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
	
	public static void printHex(byte[] bytes) {
		for(int i = 0 ; i <  bytes.length; i++)
			System.out.print(Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ");
		System.out.println();
	}
}
