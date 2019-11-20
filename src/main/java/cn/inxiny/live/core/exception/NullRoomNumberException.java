package cn.inxiny.live.core.exception;

/**
 * Created by huxins on 2019/11/19 22:43
 */
public class NullRoomNumberException extends RuntimeException {

    public NullRoomNumberException() {
        super();
    }

    public NullRoomNumberException(String message) {
        super(message);
    }

    public NullRoomNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public NullRoomNumberException(Throwable cause) {
        super(cause);
    }

    protected NullRoomNumberException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
