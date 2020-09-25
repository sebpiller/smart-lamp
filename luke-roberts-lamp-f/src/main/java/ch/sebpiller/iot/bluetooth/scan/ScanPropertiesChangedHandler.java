package ch.sebpiller.iot.bluetooth.scan;

import com.github.hypfvieh.bluetooth.DeviceManager;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ScanPropertiesChangedHandler extends AbstractPropertiesChangedHandler {
    private static final String DEVICE_IFNAME = "org.bluez.Device1";
    private static final Logger LOG = LoggerFactory.getLogger(ScanPropertiesChangedHandler.class);

    private final String bluetoothAdapter;
    private final String prefix;

    private final BlockingQueue<ScanPropertiesChangedData> queue = new LinkedBlockingQueue<>();
    private final Thread scanPropertiesChangedThread;

    private final ScanHandler scanHandler = new LogScanHandler();

    public ScanPropertiesChangedHandler(DeviceManager deviceManager, String bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        prefix = "/org/bluez/" + bluetoothAdapter + "/dev_";

        scanPropertiesChangedThread = new ScanPropertiesChangedThread(deviceManager, bluetoothAdapter, queue, scanHandler);
        scanPropertiesChangedThread.setDaemon(true);
        scanPropertiesChangedThread.start();
    }

    @Override
    public void handle(Properties.PropertiesChanged properties) {
        LOG.trace("  > {} path:{} sig:{} interface:{}", bluetoothAdapter, properties.getPath(), properties.getName(), properties.getInterfaceName());
        if (!properties.getPath().startsWith(prefix) || !properties.getInterfaceName().equals(DEVICE_IFNAME)) {
            return;
        }

        String address = properties.getPath().replace(prefix, "").replaceAll("_", ":").trim();
        if (!address.matches("^[0-9a-zA-Z:]+$")) {
            return;
        }

        queue.offer(new ScanPropertiesChangedData(address, properties, LocalDateTime.now()));
    }

}
