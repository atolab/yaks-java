package is.yaks.socket.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VLEEncoder 
{	
	private static VLEEncoder instance;
	/** 0x80 => 128 dec => 0b10000000 */
	public static final int  MORE_BYTES_FLAG = 0x80;
	/** 0x7F	=> 0b1111111 */
	public static final byte BYTE_MASK 		 = 0x7F;
	/** 7 => 0b111 */
	public static final byte SHIFT_LEN 		 = 7;
	/** 64 => 0b1000000 */
	public static final byte MAX_BITS 		 = 64;
	/** 10 =>  0b1010 */
	public static final byte MAX_BYTES		 = 10;
	
	public static final int MAX_BYTES_MASK   = 0xFF;
	
	// private constructor
	private VLEEncoder(){}
	
	public static synchronized VLEEncoder getInstance() 
	{
		if( instance == null ) 
		{
			instance = new VLEEncoder();
		}
		return instance;
	} 
	
	public static byte[] encode(int input) 
	{	
		// first find out how many bytes we need to represent the integer
		int length = ((32 - Integer.numberOfLeadingZeros(input)) + 6) / 7;
		// if the integer is 0, we still need 1 byte
		length = length > 0 ? length : 1;

		byte[] output = new byte[length];
		// for each byte of output ...		
		for(int i = 0; i < length; i++) 
		{
			// ... take the least significant 7 bits of input and set the MSB to 1 ...
			output[i] = (byte) ((input & 0b1111111) | 0b10000000);
			// ... shift the input right by 7 places, discarding the 7 bits we just used			
			input >>= 7;
		}
		// finally reset the MSB on the last byte
		output[length-1] &= 0b01111111; 
		return output;
	}
	
	public static int decode(byte[] buff) 
	{				
		int value = 0;
		
		ByteBuffer bb = ByteBuffer.wrap(buff).order(ByteOrder.BIG_ENDIAN);
		
		for(int i =0; i < buff.length; i++) 
		{		     
			int v = bb.get(i) & 0x7F;
		     
		    value = value | (v << (7 * i));	
		}
		
		return value;
	}
}
