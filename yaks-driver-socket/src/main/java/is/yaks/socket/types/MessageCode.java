package is.yaks.socket.types;

public enum MessageCode {

    LOGIN(0x01), LOGOUT(0x02), WORKSPACE(0X03),

    PUT(0xA0), UPDATE(0xA1), GET(0xA2), DELETE(0xA3),

    SUB(0xB0), UNSUB(0xB1), NOTIFY(0xB2),

    REG_EVAL(0xC0), UNREG_EVAL(0xC1), EVAL(0xC2),

    OK(0xD0), VALUES(0xD1),

    ERROR(0xE0);

    private int value;

    private MessageCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
