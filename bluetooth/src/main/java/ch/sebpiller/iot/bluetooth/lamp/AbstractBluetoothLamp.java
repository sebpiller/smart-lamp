package ch.sebpiller.iot.bluetooth.lamp;

import ch.sebpiller.iot.bluetooth.BluetoothException;
import ch.sebpiller.iot.bluetooth.BluetoothHelper;
import ch.sebpiller.iot.lamp.impl.AbstractLampBase;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;
import static ch.sebpiller.iot.bluetooth.BluetoothHelper.findDeviceOnAdapter;

/**
 * Base of a smart lamp that supports BLE connectivity (Bluetooth Low Energy).
 */
public abstract class AbstractBluetoothLamp extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBluetoothLamp.class);

    protected final BluetoothGattCharacteristic retrieveCharacteristic(String adapter, String mac, String serviceUuid, String characUuid, Map<DiscoveryFilter, Object> filter) throws BluetoothException {
        try {
            DeviceManager manager = discoverDeviceManager();
            // TODO find out if the filter is really useful here
            if (filter != null && !filter.isEmpty()) {
                manager.setScanFilter(filter);
            }

            // *****************
            BluetoothDevice device = findDeviceOnAdapter(manager, adapter, mac);
            if (device == null) {
                throw new BluetoothException("can not find device " + mac + "@" + adapter);
            }

            // *****************
            BluetoothGattService service = device.getGattServiceByUuid(serviceUuid);
            if (service == null) {
                throw new BluetoothException("unable to connect to service " + serviceUuid + ": maybe the device is out of range, or has not been connected?");
            }
            LOG.info("found service {} at UUID {}", service, serviceUuid);

            // *****************
            BluetoothGattCharacteristic charac = service.getGattCharacteristicByUuid(characUuid);
            if (charac == null) {
                throw new BluetoothException("unable to connect to characteristic " + characUuid + ": maybe the device is out of range, or has not been connected?");
            }

            LOG.info("found characteristic {} at UUID {}/{}", charac, characUuid, serviceUuid);
            if (LOG.isDebugEnabled()) {
                LOG.debug("  > inner structure: {}", ReflectionToStringBuilder.reflectionToString(charac));
            }

            return charac;
        } catch (DBusException | DBusExecutionException e) {
            throw new BluetoothException("dbus error trying to retrieve characteristic " + serviceUuid + "/" + characUuid + " on device " + mac + "@" + adapter + ": " + e, e);
        }
    }

    @Override
    public void close() {
        try {
            BluetoothHelper.discoverDeviceManager().closeConnection();
        } finally {
            super.close();
        }
    }
}
