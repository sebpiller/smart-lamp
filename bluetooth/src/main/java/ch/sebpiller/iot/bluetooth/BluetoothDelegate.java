package ch.sebpiller.iot.bluetooth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Interface of a class that can write bytes to a bluetooth device.
 * Autocloseable by default to make it easier to code implementations that require some closing mechanism.
 */
public interface BluetoothDelegate extends AutoCloseable {
    Logger _LOG = LoggerFactory.getLogger(BluetoothDelegate.class);

    @Override
    default void close() throws Exception {
        // no-op by default. Override if needed.
    }

    /**
     * Run a callable function a few times until it succeed, or until it has run #maxRetries times. If the call didn't
     * success, it either throws the exception (wraps it in a {@link RuntimeException} if needed) (if it is not in the
     * #retryExceptions list) or retry the call.
     *
     * @param call            The call to invoke.
     * @param maxRetries      Maximum number of retries.
     * @param retryExceptions what exceptions should lead to retry. Default: any exception
     * @return The result of the call if any.
     */
    static <T> T retry(Callable<T> call, int maxRetries, Class<? extends Exception>... retryExceptions) {
        Class<? extends Exception>[] re = retryExceptions.length == 0 ? new Class[]{Exception.class} : retryExceptions;

        T result = null;
        Exception lastException = null;

        int failedCount = 0;
        boolean success = false;
        boolean fatal = false;

        do {
            try {
                result = call.call();
                success = true;
            } catch (Exception e) {
                failedCount++;
                lastException = e;

                if (Arrays.stream(re).noneMatch(tClass -> tClass.isAssignableFrom(e.getClass())))
                    fatal = true;
                else if (_LOG.isWarnEnabled())
                    _LOG.warn("FAILED - Command failed on retry {} of {}", failedCount, maxRetries);
            }
        } while (!success && !fatal && failedCount < maxRetries);

        if (success) {
            return result;
        }

        throw lastException instanceof RuntimeException ?
                ((RuntimeException) lastException) :
                new RuntimeException(lastException);
    }

    /**
     * Writes #bytes to the device, throwing {@link BluetoothException} if anything bad happens.
     *
     * @param bytes The content to write to the device.
     * @throws BluetoothException if anything bad happens.
     */
    void write(byte... bytes) throws BluetoothException;

}
