package ch.sebpiller.iot.bluetooth.bluez;

import ch.sebpiller.iot.bluetooth.BluetoothException;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Helper class to centralize common tasks related to bluetooth management.
 */
public class BluetoothHelper {
    private static final Logger LOG = LoggerFactory.getLogger(BluetoothHelper.class);

    private static final String RETRY_MESSAGE = "Software caused connection abort";
    /**
     * When a "Software caused connection abort" occurs, try to reconnect at most {@value #MAX_RETRY} times.
     */
    private static final int MAX_RETRY = 3;

    public static DeviceManager discoverDeviceManager() throws BluetoothException {
        DeviceManager man;

        try {
            try {
                man = DeviceManager.getInstance();
            } catch (IllegalStateException e) {
                try {
                    man = DeviceManager.createInstance(false);
                } catch (DBusException | DBusExecutionException e1) {
                    throw new BluetoothException("could not create bluetooth device manager: " + e1, e1);
                }
            }
        } catch (UnsatisfiedLinkError ule) {
            // most likely a native dependency problem. either bluez not installed
            throw new BluetoothException("a link error occurred during bluetooth initialization. The reason is either " +
                    "that bluez is not installed, or you are running this code on an unsupported platform.: " + ule, ule);
        }

        return Objects.requireNonNull(man, "no device manager can be acquired");
    }

    /**
     * Print all gathered bluetooth information to info level, and detailed infos to debug in a slf4j implementation.
     */
    public static void printBluetoothEnvironment() throws BluetoothException {
        try {
            LOG.info("========= BT BLE environment =========");

            DeviceManager manager = discoverDeviceManager();
            LOG.info("dbus connection status: {}", manager.getDbusConnection().isConnected() ? "connected" : "disconnected");

            LOG.info("looking for installed BT adapters... ");
            manager.getAdapters().forEach(a -> LOG.info("  > found adapter {} - {}", a.getDeviceName(), a));

            LOG.info("looking for available devices, services and characteristics... ");
            manager.scanForBluetoothDevices(3_000).forEach(btd -> {
                LOG.info("found device at address {} - {}", btd.getAddress(), btd);

                btd.getGattServices().forEach(s -> {
                    LOG.info("  > has a service {}", s.getDbusPath());
                    LOG.debug("    ... with delegation to {}{}", s.getService().isRemote() ? "remote " : "", s.getService().getObjectPath());

                    s.getGattCharacteristics().forEach(c -> {
                        LOG.info("    > charac {} with flags {}", c, c.getFlags());

                        c.getGattDescriptors().forEach(gd -> LOG.debug("      > gatt descriptor {} with flags {}", gd, gd.getFlags()));
                    });
                });
            });

            LOG.info("========= END OF SCAN ================");
        } catch (DBusExecutionException e) {
            throw new BluetoothException("Error during scan: " + e, e);
        }
    }

    public static void reconnectIfNeeded(BluetoothGattCharacteristic charac) {
        if (charac == null) {
            if (LOG.isTraceEnabled())
                LOG.trace("charac is null");
            return;
        }

        boolean quit = false;
        int tryIndex = 0;
        RuntimeException lastError = null;

        while (!quit) {
            tryIndex++;

            try {
                BluetoothDevice device = charac.getService().getDevice();
                if (!device.isConnected() && !device.connect()) {
                    throw new BluetoothException("!!! connection to the device was unsuccessful !!!");
                }

                quit = true;
            } catch (RuntimeException e) {
                lastError = e;
                quit = true;

                if (e.getMessage().contains(RETRY_MESSAGE)) {
                    quit = tryIndex < MAX_RETRY;
                }
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }

    public static BluetoothDevice findDeviceOnAdapter(DeviceManager manager, String localBtAdapter, String remoteDeviceMac) throws BluetoothException {
        // TODO implement a cache of devices ?
        LOG.info("searching for device {} on {}", remoteDeviceMac, localBtAdapter);
        if (LOG.isDebugEnabled()) {
            LOG.debug("  > dbus connection status: {}", manager.getDbusConnection().isConnected() ? "connected" : "disconnected");
        }

        // TODO implement optional properties changed handler
        //manager.registerPropertyHandler(new ScanPropertiesChangedHandler(manager, localBtAdapter));

        // Power on the adapter if needed, fails if not found
        BluetoothAdapter adapter = manager.getAdapters().stream()
                .filter(a -> Objects.equals(a.getDeviceName(), localBtAdapter))
                .findFirst()
                .orElseThrow(() -> new BluetoothException("bluetooth adapter " + localBtAdapter + " can not be found"));
        if (!adapter.isPowered()) {
            adapter.setPowered(true);
            LOG.debug("  > switched {} on", localBtAdapter);
        }

        // search for a connected device
        return manager.getDevices(true).stream()
                .filter(e -> Objects.equals(e.getAddress(), remoteDeviceMac))
                .findFirst()
                .orElseThrow(() -> new BluetoothException("device " + remoteDeviceMac + " is not registered. Please use 'bluetoothctl' to trust/connect this device."));
    }
}
