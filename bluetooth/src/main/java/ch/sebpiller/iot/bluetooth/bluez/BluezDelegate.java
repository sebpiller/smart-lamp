package ch.sebpiller.iot.bluetooth.bluez;

import ch.sebpiller.iot.bluetooth.BluetoothDelegate;
import ch.sebpiller.iot.bluetooth.BluetoothException;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Connector for Bluetooth BLE, backed with the API Bluez.
 */
public final class BluezDelegate implements BluetoothDelegate {
    private static final Logger LOG = LoggerFactory.getLogger(BluezDelegate.class);

    private final String btAdapter;
    private final String macAddr;
    private final UUID serviceUuid;
    private final UUID characUuid;

    private BluetoothDevice device;

    /**
     * The Bluetooth endpoint to invoke to control the lamp.
     */
    private BluetoothGattCharacteristic externalApi;

    public BluezDelegate(String btAdapter, String macAddr, UUID serviceUuid, UUID characUuid) {
        this.btAdapter = btAdapter;
        this.macAddr = macAddr;
        this.serviceUuid = serviceUuid;
        this.characUuid = characUuid;
    }


    private final BluetoothGattCharacteristic retrieveCharacteristic(String adapter, String mac, String serviceUuid, String characUuid, Map<DiscoveryFilter, Object> filter) throws BluetoothException {
        try {
            BluetoothGattService service = getBluetoothDevice(adapter, mac, filter).getGattServiceByUuid(serviceUuid);
            if (service == null) {
                throw new BluetoothException("unable to connect to service " + serviceUuid + ": maybe the device is out of range, or has not been connected?");
            }
            LOG.info("found service {} at UUID {}", service, serviceUuid);

            BluetoothGattCharacteristic charac = service.getGattCharacteristicByUuid(characUuid);
            if (charac == null) {
                throw new BluetoothException("unable to connect to characteristic " + characUuid + ": maybe the device is out of range, or has not been connected?");
            }
            LOG.info("found characteristic {} at UUID {}/{}", charac, characUuid, serviceUuid);
            if (LOG.isDebugEnabled()) {
                LOG.debug("  > inner structure: {}", ToStringBuilder.reflectionToString(charac));
            }

            return charac;
        } catch (DBusException | DBusExecutionException e) {
            throw new BluetoothException(
                    String.format("dbus error trying to retrieve characteristic %s/%s on device %s@%s: %s", serviceUuid, characUuid, mac, adapter, e),
                    e
            );
        }
    }

    private BluetoothGattCharacteristic getExternalApi() {
        if (this.externalApi == null) {
            EnumMap<DiscoveryFilter, Object> filter = new EnumMap<>(DiscoveryFilter.class);
            filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
            filter.put(DiscoveryFilter.UUIDs, new String[]{
                    this.serviceUuid.toString()
            });

            this.externalApi = retrieveCharacteristic(
                    this.btAdapter,
                    this.macAddr,
                    this.serviceUuid.toString(),
                    this.characUuid.toString(),
                    filter
            );
        }

        return this.externalApi;
    }

    private BluetoothDevice getBluetoothDevice(String adapter, String mac, Map<DiscoveryFilter, Object> filter) throws BluezInvalidArgumentsException, BluezNotReadyException, BluezNotSupportedException, BluezFailedException {
        DeviceManager manager = BluetoothHelper.discoverDeviceManager();
        if (filter != null && !filter.isEmpty()) {
            manager.setScanFilter(filter);
        }

        this.device = BluetoothHelper.findDeviceOnAdapter(manager, adapter, mac);
        if (this.device == null || !this.device.connect()) {
            throw new BluetoothException("can not connect to device " + mac + "@" + adapter);
        }

        return this.device;
    }

    @Override
    public void close() {
        if (this.device != null) {
            this.device.disconnect();
        }
    }

    @Override
    public void write(byte... bytes) {
        Validate.isTrue(ArrayUtils.isNotEmpty(bytes), "empty command received");

        BluetoothGattCharacteristic api = getExternalApi();
        BluetoothHelper.reconnectIfNeeded(api);

        if (LOG.isTraceEnabled()) {
            BluetoothDevice dev = api.getService().getDevice();
            LOG.trace("sending {} bytes to BlueZ API '{}' ({})",
                    bytes.length,
                    dev.getName(),
                    dev.getAddress()
            );
        }

        try {
            api.writeValue(bytes, Collections.emptyMap());
        } catch (DBusException e) {
            throw new BluetoothException(e);
        }
    }
}
