package ch.sebpiller.iot.bluetooth.scan;

import com.github.hypfvieh.DbusHelper;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.bluez.Device1;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

public final class ScanPropertiesChangedThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ScanPropertiesChangedThread.class);
    private static int threadId;

    private final Map<String, BluetoothDevice> deviceMap = new HashMap<>();
    private final Map<String, String> nameMap = new HashMap<>();

    private final BlockingQueue<ScanPropertiesChangedData> queue;
    private final String bluetoothAdapter;
    private final DeviceManager manager;
    private final ScanHandler scanHandler;

    public ScanPropertiesChangedThread(
            DeviceManager manager,
            String bluetoothAdapter,
            BlockingQueue<ScanPropertiesChangedData> queue,
            ScanHandler scanHandler) {
        this.manager = Objects.requireNonNull(manager);
        this.bluetoothAdapter = Objects.requireNonNull(bluetoothAdapter);
        this.queue = Objects.requireNonNull(queue);
        this.scanHandler = Objects.requireNonNull(scanHandler);
        setName(getClass().getSimpleName() + '-' + (++threadId));
    }

    public void run() {
        while (true) {
            try {
                ScanPropertiesChangedData data = queue.take();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("***************************");
                    LOG.trace("found properties changed: ");
                    LOG.trace("{}", ReflectionToStringBuilder.reflectionToString(data));
                    LOG.trace("***************************");
                }

                if (!deviceMap.containsKey(data.address)) {
                    Device1 device = DbusHelper.getRemoteObject(manager.getDbusConnection(), data.properties.getPath(), Device1.class);
                    deviceMap.put(data.address, new BluetoothDevice(device, manager.getAdapter(), data.properties.getPath(), manager.getDbusConnection()));
                    LOG.debug("{} {} added to deviceMap.", bluetoothAdapter, data.address);
                }

                int rssi = ScanData.RSSI_UNSET;
                int txPower = ScanData.TXPOWER_UNSET;

                Map<String, Variant<?>> map = data.properties.getPropertiesChanged();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    LOG.debug("  > found entry: {}={}", entry.getKey(), ReflectionToStringBuilder.reflectionToString(entry.getValue()));
                }

                for (Map.Entry<String, Variant<?>> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Variant<?> value = entry.getValue();

                    LOG.trace("{} address:{} key:{} class:{} sig:{} type:{} value:{}", bluetoothAdapter,
                            data.address, key, value.getClass().getName(), value.getSig(), value.getType(), value.getValue());

                    if ("RSSI".equals(key)) {
                        rssi = ((Short) (value.getValue())).intValue();
                    } else if ("TxPower".equals(key)) {
                        txPower = ((Short) (value.getValue())).intValue();
                    }
                }

                if (!nameMap.containsKey(data.address)) {
                    nameMap.put(data.address, deviceMap.get(data.address).getName());
                    LOG.debug("{} {} ({}) added to nameMap.", bluetoothAdapter, data.address, nameMap.get(data.address));
                }

                ScanData scanData = new ScanData(bluetoothAdapter, data.address, nameMap.get(data.address), rssi, txPower, data.date);
                scanHandler.handle(deviceMap.get(data.address), scanData);
            } catch (InterruptedException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.debug("  interrupted - {}", e.toString(), e);
                }
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
