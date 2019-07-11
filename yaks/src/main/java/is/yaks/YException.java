package is.yaks;

public class YException extends Exception {

    protected YException(String message) {
        super(message);
    }

    protected YException(String message, Throwable cause) {
        super(message, cause);
    }

}