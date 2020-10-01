package ch.sebpiller.iot.bluetooth;

import ch.sebpiller.iot.bluetooth.scan.ScanPropertiesChangedHandler;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Helper class to centralize common tasks releated to bluetooth management.
 */
public class BluetoothHelper {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothHelper.class);

    public static DeviceManager discoverDeviceManager() {
        DeviceManager man;

        try {
            try {
                man = DeviceManager.getInstance();
            } catch (IllegalStateException e) {
                LOG.trace("caught: " + e.toString(), e);

                try {
                    man = DeviceManager.createInstance(false);
                } catch (DBusException e1) {
                    throw new IllegalStateException(e1);
                }
            }
        } catch (UnsatisfiedLinkError ule) {
            // most likely a native dependency problem. either bluez not installed, or misconfigured
            throw new IllegalStateException("was unable to access native libraries. Please install the dependencies required (bluez): " + ule, ule);
        }

        return Objects.requireNonNull(man, "no device manager can be acquired");
    }

    /**
     * Print all gathered bluetooth information to info level, and detailed infos to debug in a slf4j implementation.
     */
    public static void printBluetoothEnvironment() {
        LOG.info("========= BT BLE environment =========");

        DeviceManager manager = discoverDeviceManager();
        LOG.info("dbus connection status: {}", manager.getDbusConnection().isConnected() ? "connected" : "disconnected");

        LOG.info("looking for installed BT adapters... ");
        manager.getAdapters().stream().forEach(a -> LOG.info("  > found adapter {} - {}", a.getDeviceName(), a));

        LOG.info("looking for available devices, services and characteristics... ");
        manager.scanForBluetoothDevices(3_000).stream().forEach(btd -> {
            LOG.info("found device at address {} - {}", btd.getAddress(), btd);

            btd.getGattServices().stream().forEach(s -> {
                LOG.info("  > has a service {}", s.getDbusPath());
                LOG.debug("    ... with delegation to {}{}", s.getService().isRemote() ? "remote " : "", s.getService().getObjectPath());

                s.getGattCharacteristics().stream().forEach(c -> {
                    LOG.info("    > charac {} with flags {}", c, c.getFlags());

                    c.getGattDescriptors().stream().forEach(gd -> {
                        LOG.debug("      > gatt descriptor {} with flags {}", gd, gd.getFlags());
                    });
                });
            });
        });

        LOG.info("========= END OF SCAN ================");
    }

    public static void reconnectIfNeeded(BluetoothGattCharacteristic charac) {
        if(charac == null) {
            LOG.info("can not reconnect a null object");
        } else {
            BluetoothDevice device = charac.getService().getDevice();

            if (!device.isConnected() && !device.connect()) {
                throw new IllegalStateException("!!! connection to the device was unsuccessful !!!");
            }
        }
    }

    public static BluetoothDevice findDeviceOnAdapter(DeviceManager manager, String localBtAdapter, String remoteDeviceMac) throws DBusException {
        LOG.info("searching for device {} on {}", localBtAdapter, remoteDeviceMac);
        if (LOG.isDebugEnabled()) {
            LOG.debug("  > dbus connection status: {}", manager.getDbusConnection().isConnected() ? "connected" : "disconnected");
        }
        manager.registerPropertyHandler(new ScanPropertiesChangedHandler(manager, localBtAdapter));

        BluetoothAdapter adapter = manager.getAdapters().stream()
                .filter(a -> Objects.equals(a.getDeviceName(), localBtAdapter))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("bluetooth adapter " + localBtAdapter + " can not be found"));
        if (!adapter.isPowered()) {
            adapter.setPowered(true);
            LOG.debug("  > switched on {}", localBtAdapter);
        }

        return manager.getDevices(true).stream()
                .filter(e -> Objects.equals(e.getAddress(), remoteDeviceMac))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("device " + remoteDeviceMac + " is not registered. Please use 'bluetoothctl' in bash to trust/connect this device."));

    }
}
