package is.yaks;

import java.nio.ByteBuffer;

public class Value {
	
	private Encoding encoding;
	
	private ByteBuffer value;

	public Encoding getEncoding() {
		return encoding;
	}

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
	}

	public ByteBuffer getValue() {
		return value;
	}

	public void setValue(ByteBuffer value) {
		this.value = value;
	}
	

}
