package is.yaks.socket.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

import is.yaks.Encoding;
import is.yaks.Path;
import is.yaks.Value;

public class Utils {

 //   public static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    public static final String IS_YAKS_ACCESS = "is.yaks.access";
    public static final String IS_YAKS_STORAGE = "is.yaks.storage";

    private Utils() {
        // nothing to do, just hides the implicit public one
    }

    public static String getHeader(MultivaluedMap<String, String> headers, String cookie) {
        List<String> setCookie = headers.get(cookie);
        if (setCookie != null && !setCookie.isEmpty()) {
            String value = setCookie.get(0);
            if (value != null && !value.isEmpty()) {
                return value;
            } else {
                fail("No value in cookie");
            }
        } else {
            fail("No cookie");
        }

        return null;
    }

    public static String getValueFromHeaderKey(MultivaluedMap<String, String> headers, String cookie, String field) {
        List<String> cookieList = headers.get(cookie);
        if (cookieList != null && !cookieList.isEmpty()) {
            String firstCookie = cookieList.get(0);
            if (firstCookie != null && !firstCookie.isEmpty()) {
                String[] splittedString = firstCookie.split("=");
                if (splittedString[0].equals(field)) {
                    return splittedString[1];
                } else {
                    fail("Invalid field in cookie: " + splittedString[0] + " != " + field);
                }
            } else {
                fail("No value in cookie: " + firstCookie);
            }
        } else {
            fail("No cookie: " + headers);
        }

        return null;
    }

    public static void fail(String string) {
        try {
            throw new RuntimeException(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String stringify(String text) {
        return "_" + text.replaceAll("\\-", "_");
    }
    
	public static int generate_correlation_id()
	{		
		Random random = new Random();
		return random.nextInt();
	}
	
	public static ByteBuffer mapToByteBuffer(Map<String, String> map) 
	{  
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.put((byte)map.size()); // put the size of the properties map
		
		Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
		while(entries.hasNext()) 
		{
		    Map.Entry<String, String> entry = entries.next();
		    buffer.put((byte) entry.getKey().length()); 
		    buffer.put(entry.getKey().getBytes()); 
		    buffer.put((byte) entry.getValue().length());
		    buffer.put(entry.getValue().getBytes());
		}	
		buffer.flip();
		return buffer;  
	}
	
	public static ByteBuffer mapDataToByteBuffer(Map<String, String> map) 
	{  
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.put((byte)map.size()); // put the size of the properties map
		
		Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
		while(entries.hasNext()) 
		{
		    Map.Entry<String, String> entry = entries.next();
		    buffer.put((byte) entry.getKey().length()); 
		    buffer.put(entry.getKey().getBytes()); 
		    buffer.put((byte) Encoding.RAW.getIndex()); //Encoding.RAW = 0x01
		    buffer.put((byte) entry.getValue().length());
		    buffer.put(entry.getValue().getBytes());
		}	
		buffer.flip();
		return buffer;  
	} 

	public static ByteBuffer porpertiesListToByteBuffer(Map<String, String> propertiesList) 
	{   
		ByteBuffer buffer = ByteBuffer.allocate(512);
	//	buffer.put((byte)propertiesList.size()); 		// put the size of the map i.e. 0x01 
		buffer.put((byte) 0x00);
		
		for (Map.Entry<String, String> entry : propertiesList.entrySet())
		{
		    String key = entry.getKey();
		    String val = entry.getValue();
		 //   buffer.put((byte)key.length());
		    buffer.put(key.getBytes());
		    buffer.put("=".getBytes());
		    buffer.put(val.getBytes());
		}
		
		// adding the property length
		buffer.flip();
		int limit = buffer.limit();
		buffer.put(0, (byte) (limit-1));
		
//		buffer.flip();
		return buffer;  
	} 
	
	public static ByteBuffer workspaceListToByteBuffer(Map<Path, Value> wkspList) 
	{   
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.put((byte)wkspList.size()); 		// put the size of the map i.e. 0x01 
		
		for (Map.Entry<Path, Value> entry : wkspList.entrySet())
		{
		    Path key = (Path)entry.getKey();
		    Value val = (Value)entry.getValue();
		    String v = val.getValue().toString();
			if(!val.getEncoding().equals(Encoding.JSON)) {
			    if(v.contains("{") && v.contains("}")) {
				    v = v.substring(v.indexOf("{")+1, v.indexOf("}"));
				}
			} 
		    buffer.put((byte)key.toString().length());
		    buffer.put(key.toString().getBytes());
		    buffer.put((byte)val.getEncoding().getIndex());
		    if(val.getEncoding().getIndex() == Encoding.RAW.getIndex()) {
		    	buffer.put((byte)val.getEncoding().getIndex());
		    	buffer.put(Encoding.get_raw_format().getBytes());
		    }
		    buffer.put((byte)v.length());
		    buffer.put(v.getBytes());
		}
		buffer.flip();
		return buffer;  
	} 
	
	public static ByteBuffer listDataToByteBuffer(List<String> data) 
	{   
		ByteBuffer buffer = ByteBuffer.allocate(512);
	//	buffer.put((byte)data.size()); 		// put the size of the data list 0x01 no need
		
		Iterator<String> entries = data.iterator();
		while(entries.hasNext()) 
		{
		    String entry = entries.next();
		    buffer.put((byte) entry.length());
		    buffer.put(entry.getBytes()); 
		}	
		buffer.flip();
		return buffer;  
	} 
	
	
	public static String decodeByteArrayToString(byte[] msg) 
	{
		byte[] msg_ = new byte[msg.length];
		
		for (int i =0 ; i < msg.length; i++) 
		{
			msg_[i] = (byte) (msg[i] & 0xFF);
		}
		return new String(Base64.getDecoder().decode(msg_));
	}
	
	
	public static byte[] toByteArray(int value) {
	    return new byte[] {
	            (byte)(value >> 24),
	            (byte)(value >> 16),
	            (byte)(value >> 8),
	            (byte)value};
	}
	
	public static void printHex(byte[] bytes) {
		for(int i = 0 ; i <  bytes.length; i++)
			System.out.print(Integer.toHexString(Byte.toUnsignedInt(bytes[i])) + " ");
		System.out.println();
	}
	
	
}
