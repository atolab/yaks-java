package is.yaks.socketfe;

public enum MessageFlags 
{
	PROPERTY(0x01),
	STORAGE(0x02),
	ACCESS(0x04),
	ENCODING(0x38),
	ENCODING_RAW(0x08),
	ENCODING_JSON(0x10),
	ENCODING_PROTO(0x18);
	
	private int value;
	
	private MessageFlags(int value) 
	{
		this.value = value;
	}
	
	public int getValue() 
	{
		return value;
	}
}
