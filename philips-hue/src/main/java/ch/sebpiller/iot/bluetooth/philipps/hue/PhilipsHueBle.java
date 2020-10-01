package ch.sebpiller.iot.bluetooth.philipps.hue;

import ch.sebpiller.iot.bluetooth.BluetoothHelper;
import ch.sebpiller.iot.bluetooth.lamp.AbstractBluetoothLamp;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;

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
// WARN : the mac address of an Hue changes randomly at each "factory reset" of the bulb !
public class PhilipsHueBle extends AbstractBluetoothLamp {
    private static final Logger LOG = LoggerFactory.getLogger(PhilipsHueBle.class);

    //932c32bd-0002-47a2-835a-a8d455b859dd
    private static final String PHILIPS_SERVICE_UUID = "0000fe0f-0000-1000-8000-00805f9b34fb";
    private static final String PHILIPS_CHARAC_UUID = "0000fe0f-0000-1000-8000-00805f9b34fb";

    private final Map<DiscoveryFilter, Object> filter;

    private final String adapter, mac;
    private BluetoothGattCharacteristic characteristic;

    public PhilipsHueBle(String adapter, String mac) {
        this.adapter = adapter;
        this.mac = mac;

        filter = new HashMap<>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
        filter.put(DiscoveryFilter.UUIDs, new String[]{PHILIPS_SERVICE_UUID});
    }

    public PhilipsHueBle(String mac) {
        this("hci0", mac);
    }

    @Override
    public PhilipsHueBle power(boolean on) {
        // TODO
        byte[] ba = new byte[]{};
        sendCommandToExternalApi(ba);
        return this;
    }

    @Override
    public PhilipsHueBle setBrightness(byte percent) {
        // TODO
        return this;
    }

    @Override
    public PhilipsHueBle setTemperature(int kelvin) {
        // TODO
        return this;
    }

    @Override
    public PhilipsHueBle setScene(byte scene) throws UnsupportedOperationException {
        // TODO
        return this;
    }

    private BluetoothGattCharacteristic getPhilipsCommandApi() {
        if (characteristic == null) {
            characteristic = discoverBluetoothCharacteristic(
                    discoverDeviceManager(),
                    adapter,
                    mac,
                    PHILIPS_SERVICE_UUID,
                    PHILIPS_CHARAC_UUID,
                    filter
            );
        }

        return characteristic;
    }

    private void sendCommandToExternalApi(byte[] o) {
        try {
            BluetoothGattCharacteristic api = getPhilipsCommandApi();
            BluetoothHelper.reconnectIfNeeded(api);

            if (LOG.isDebugEnabled()) {
                BluetoothDevice device = api.getService().getDevice();
                LOG.debug("sending command {} to Philips Hue '{}' ({})",
                        o,
                        device.getName(),
                        device.getAddress()
                );
            }

            api.writeValue(o, Collections.emptyMap());
        } catch (DBusException e) {
            throw new IllegalStateException("unable to invoke command on Philips Hue: " + e, e);
        }
    }


    @Override
    public void close() {
        try {
            characteristic = null;
        } finally {
            super.close();
        }
    }
}
