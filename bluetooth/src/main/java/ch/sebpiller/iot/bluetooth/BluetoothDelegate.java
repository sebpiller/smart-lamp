package ch.sebpiller.iot.bluetooth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Callable;

public interface BluetoothDelegate extends AutoCloseable {
    Logger LOG = LoggerFactory.getLogger(BluetoothDelegate.class);

    /**
     * Retry to run a function a few times, retry if specific exceptions occur.
     *
     * @param retryExceptions what exceptions should lead to retry. Default: any exception
     */
    static <T> T retry(Callable<T> call, int maxRetries, Class<? extends Exception>... retryExceptions) {
        retryExceptions = retryExceptions.length == 0 ? new Class[]{Exception.class} : retryExceptions;
        int retryCounter = 0;
        Exception lastException = null;
        while (retryCounter < maxRetries) {
            try {
                return call.call();
            } catch (Exception e) {
                lastException = e;
                if (Arrays.stream(retryExceptions).noneMatch(tClass ->
                        tClass.isAssignableFrom(e.getClass())
                ))
                    throw lastException instanceof RuntimeException ?
                            ((RuntimeException) lastException) :
                            new RuntimeException(lastException);
                else {
                    retryCounter++;
                    LOG.warn("FAILED - Command failed on retry {} of {}", retryCounter, maxRetries);
                    if (retryCounter >= maxRetries) {
                        break;
                    }
                }
            }
        }

        throw lastException instanceof RuntimeException ?
                ((RuntimeException) lastException) :
                new RuntimeException(lastException);
    }

    void write(byte... bytes) throws BluetoothException;
}
