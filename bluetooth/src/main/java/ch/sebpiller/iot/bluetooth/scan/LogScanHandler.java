package ch.sebpiller.iot.bluetooth.scan;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LogScanHandler implements ScanHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LogScanHandler.class);

    private final Logger log;

    public LogScanHandler() {
        this(null);
    }

    public LogScanHandler(Logger log) {
        this.log = log;
    }

    @Override
    public void handle(BluetoothDevice device, ScanData data) {
        Logger l = log == null ? LOG : log;

        if (l.isInfoEnabled()) {
            l.info("****************");
            l.info("device: {}", ReflectionToStringBuilder.reflectionToString(device));
            l.info("data: {}", ReflectionToStringBuilder.reflectionToString(data));
            l.info("****************");
        }
    }
}
