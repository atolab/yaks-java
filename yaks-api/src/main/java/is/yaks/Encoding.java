package is.yaks;

public enum Encoding {
	
    BYTE_BUFFER(0x01), 
    JSON(0x02), 
    PROTOBUF(0x03), 
    AVRO(400);
	
	private int index;
	
	private Encoding(int index) 
	{
		this.index = index;
	}
	
	public int getIndex() 
	{
		return index;
	}
	
	public static Encoding getEncoding(int encodingIndex) {
		for (Encoding e : Encoding.values()) {
			if (e.index == encodingIndex) return e;
		}
		throw new IllegalArgumentException("Encoding not found.");
	}
}
