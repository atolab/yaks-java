package is.yaks.socket.messages;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

import is.yaks.Encoding;
import is.yaks.socket.utils.Utils;
import is.yaks.socket.utils.VLEEncoder;
import is.yaks.socketfe.Message;
import is.yaks.socketfe.MessageCode;

public class MessageValues implements Message {
	
	private byte[] body = { 0x00 };
	private ByteBuffer data;
	private byte[] raw_msg = { 0x00, 0x01, 0x02, 0x03, 0x04};
	//1VLE max 64bit
	private VLEEncoder encoder;
	//1VLE max 64bit
	private int correction_id;
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
	private int vle_length;
	private Encoding encoding;
	private int message_code;
	private Map<String, String> dataList;	
	private Map<String, String> propertiesList;
	

	public MessageValues() {
		super();
		// TODO Auto-generated constructor stub
	}

		
	public MessageValues(String id, String key, String value) 
	{	
		this.message_code = MessageCode.VALUES.getValue();
		this.correction_id = Utils.generate_correlation_id();
		this.propertiesList.put("'is.yaks.access.id'", id);
		this.encoding = Encoding.BYTE_BUFFER;
		this.dataList.put(key, value);
	}
	
	
	@Override
	public ByteBuffer write() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Message read(ByteBuffer bytes) {
		// TODO Auto-generated method stub
		return null;
	}

}
