package is.yaks.socket.types;

public enum ErrorCode {
    BAD_REQUEST(400), FORBIDDEN(403), NOT_FOUND(404), PRECONDITION_FAILED(412), NOT_IMPLEMENTED(
            501), INSUFFICIENT_STORAGE(507);

    private int value;

    private ErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
