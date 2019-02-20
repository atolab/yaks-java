package is.yaks;

public class Value {
	
	private Encoding encoding;
	
	private String value;

	public Value() {}
	
	public Value(String val){
		value = val;
		encoding = Encoding.RAW; //encoding by default RAW
	}
	
	public Value(String val, Encoding enc) {
		value = val;
		encoding = enc;
	}
	
	public Encoding getEncoding() {
		return encoding;
	}

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String val) {
		this.value = val;
	}
	

}
