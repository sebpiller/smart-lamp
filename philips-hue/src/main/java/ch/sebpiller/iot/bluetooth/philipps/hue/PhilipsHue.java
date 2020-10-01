package ch.sebpiller.iot.bluetooth.philipps.hue;

import ch.sebpiller.iot.lamp.impl.AbstractLampBase;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;
import static ch.sebpiller.iot.bluetooth.BluetoothHelper.findDeviceOnAdapter;

/*
Device E6:B3:DC:6E:06:32 (random)
    Name: Hue Lamp
    Alias: Hue Lamp
    Paired: no
    Trusted: no
    Blocked: no
    Connected: no
    LegacyPairing: no
    UUID: Signify Netherlands B.V.. (0000fe0f-0000-1000-8000-00805f9b34fb)
    ServiceData Key: 0000fe0f-0000-1000-8000-00805f9b34fb
    ServiceData Value:
05 10 ff ff 05                                   .....
    RSSI: -42
    TxPower: 10
 */
public class PhilipsHue extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(PhilipsHue.class);

    private String uuid = "0000fe0f-0000-1000-8000-00805f9b34fb";
    private Map<DiscoveryFilter, Object> filter;

    private final String adapter, mac;
    private BluetoothGattCharacteristic externalApi;

    public PhilipsHue(String adapter, String mac) {
        this.adapter = adapter;
        this.mac = mac;

        Map<DiscoveryFilter, Object> filter = new HashMap<>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
        filter.put(DiscoveryFilter.UUIDs, new String[]{"0000fe0f-0000-1000-8000-00805f9b34fb"});
    }
    public PhilipsHue(String mac) {
        this("hci0", mac);
    }

    @Override
    public PhilipsHue power(boolean on) {
        return this;
    }

    @Override
    public PhilipsHue setBrightness(byte percent) {
        return this;
    }

    @Override
    public PhilipsHue setTemperature(int kelvin) {
        return this;
    }

    @Override
    public PhilipsHue setScene(byte scene) throws UnsupportedOperationException {
        return this;
    }

    private BluetoothGattCharacteristic getExternalApi() {
        if (externalApi == null) {
            externalApi = discoverExternalApiEndpoint(discoverDeviceManager(), filter);
        }

        return externalApi;
    }

    private BluetoothGattCharacteristic discoverExternalApiEndpoint(DeviceManager manager, Map<DiscoveryFilter, Object> filter) {
        try {
            return tryDiscoverExternalApiEndpoint(manager, filter);
        } catch (DBusException e) {
            throw new IllegalStateException("unable to find external api endpoint: " + e, e);
        }
    }

    private BluetoothGattCharacteristic tryDiscoverExternalApiEndpoint(DeviceManager manager, Map<DiscoveryFilter, Object> filter) throws DBusException {
        // TODO find out if the filter is really useful here
        if (filter != null && !filter.isEmpty()) {
            manager.setScanFilter(filter);
        }

        BluetoothDevice philipsHue = findDeviceOnAdapter(manager, adapter, mac);
        if (philipsHue == null) {
            LOG.error("can not find Philips Hue {}@{}", adapter, mac);
            return null;
        }


        BluetoothGattService customControlService = philipsHue.getGattServiceByUuid(uuid);

        if (customControlService == null) {
            LOG.error("unable to connect to the custom control service {}: maybe the lamp is out of range.", ccSer.getUuid());
            return null;
        }
        LOG.info("found GATT custom control service {} at UUID {}", customControlService, ccSer.getUuid());


        return null;
    }
}
