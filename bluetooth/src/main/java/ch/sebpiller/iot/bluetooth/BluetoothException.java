package ch.sebpiller.iot.bluetooth;

public class BluetoothException extends RuntimeException {
    public BluetoothException() {
        super();
    }

    public BluetoothException(String message) {
        super(message);
    }

    public BluetoothException(String message, Throwable cause) {
        super(message, cause);
    }

    public BluetoothException(Throwable cause) {
        super(cause);
    }

    protected BluetoothException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
