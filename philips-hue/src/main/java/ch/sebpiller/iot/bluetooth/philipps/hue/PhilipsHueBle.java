package ch.sebpiller.iot.bluetooth.philipps.hue;

import ch.sebpiller.iot.bluetooth.BluetoothException;
import ch.sebpiller.iot.bluetooth.BluetoothHelper;
import ch.sebpiller.iot.lamp.impl.AbstractLampBase;
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
import java.util.Objects;

/*

> bluetoothctl
[bluetooth]# connect D7:0E:87:C6:D2:58
Attempting to connect to D7:0E:87:C6:D2:58
[CHG] Device D7:0E:87:C6:D2:58 Connected: yes
Connection successful
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0001
        00001801-0000-1000-8000-00805f9b34fb
        Generic Attribute Profile
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0001/char0002
        00002a05-0000-1000-8000-00805f9b34fb
        Service Changed
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0001/char0002/desc0004
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0001/char0005
        00002b2a-0000-1000-8000-00805f9b34fb
        Database Hash
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0001/char0007
        00002b29-0000-1000-8000-00805f9b34fb
        Client Supported Features
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service000e
        0000180a-0000-1000-8000-00805f9b34fb
        Device Information
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service000e/char000f
        00002a29-0000-1000-8000-00805f9b34fb
        Manufacturer Name String
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service000e/char0011
        00002a24-0000-1000-8000-00805f9b34fb
        Model Number String
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service000e/char0013
        00002a28-0000-1000-8000-00805f9b34fb
        Software Revision String
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015
        0000fe0f-0000-1000-8000-00805f9b34fb
        Signify Netherlands B.V. (formerly Philips Lighting B.V.)
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0016
        97fe6561-0001-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0001)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0018
        97fe6561-0003-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char001a
        97fe6561-0004-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char001c
        97fe6561-0008-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char001c/desc001e
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char001f
        97fe6561-1001-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char001f/desc0021
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0022
        97fe6561-2001-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0024
        97fe6561-2002-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0026
        97fe6561-2004-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0015/char0028
        97fe6561-a001-4f62-86e9-b71ee2da3d22
        Vendor specific
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a
        932c32bd-0000-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char002b
        932c32bd-0001-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char002d
        932c32bd-0002-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char002d/desc002f
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0030
        932c32bd-0003-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0030/desc0032
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0033
        932c32bd-0004-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0033/desc0035
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0036
        932c32bd-0005-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0036/desc0038
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char0039
        932c32bd-0006-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char003b
        932c32bd-0007-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char003b/desc003d
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service002a/char003e
        932c32bd-1005-47a2-835a-a8d455b859dd
        Vendor specific
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040
        b8843add-0000-4aa1-8794-c3f462030bda
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040/char0041
        b8843add-0001-4aa1-8794-c3f462030bda
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040/char0043
        b8843add-0002-4aa1-8794-c3f462030bda
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040/char0043/desc0045
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040/char0046
        b8843add-0003-4aa1-8794-c3f462030bda
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service0040/char0048
        b8843add-0004-4aa1-8794-c3f462030bda
        Vendor specific
[NEW] Primary Service (Handle 0xd904)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service004a
        9da2ddf1-0000-44d0-909c-3f3d3cb34a7b
        Vendor specific
[NEW] Characteristic (Handle 0x0304)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service004a/char004b
        9da2ddf1-0001-44d0-909c-3f3d3cb34a7b
        Vendor specific
[NEW] Descriptor (Handle 0x82c8)
        /org/bluez/hci0/dev_D7_0E_87_C6_D2_58/service004a/char004b/desc004d
        00002902-0000-1000-8000-00805f9b34fb
        Client Characteristic Configuration
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 00001800-0000-1000-8000-00805f9b34fb
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 00001801-0000-1000-8000-00805f9b34fb
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 0000180a-0000-1000-8000-00805f9b34fb
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 0000fe0f-0000-1000-8000-00805f9b34fb
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 932c32bd-0000-47a2-835a-a8d455b859dd
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: 9da2ddf1-0000-44d0-909c-3f3d3cb34a7b
[CHG] Device D7:0E:87:C6:D2:58 UUIDs: b8843add-0000-4aa1-8794-c3f462030bda
[CHG] Device D7:0E:87:C6:D2:58 ServicesResolved: yes
 */
/*
932c32bd-0000-47a2-835a-a8d455b859dd and characteristic 932c32bd-0002-47a2-835a-a8d455b859dd, I can send bytes 0x01
and 0x00 to turn the bulb on and off respectively. Brightness can be set using characteristic **0002**.
*/

// FIXME implement
// WARN : the mac address of an Hue changes randomly at each "factory reset" of the bulb !
public class PhilipsHueBle extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(PhilipsHueBle.class);

    //
    private static final String PHILIPS_PRIMARY_SERVICE_UUID = "932c32bd-0000-47a2-835a-a8d455b859dd";
    private static final String PHILIPS_POWER_CHARAC_UUID = "932c32bd-0002-47a2-835a-a8d455b859dd";
    private static final String PHILIPS_BRIGHTNESS_CHARAC_UUID = "932c32bd-0003-47a2-835a-a8d455b859dd";
    private static final String PHILIPS_COLOR_CHARAC_UUID = "932c32bd-0004-47a2-835a-a8d455b859dd";
    //private static final String PHILIPS_COLOR_CHARAC_UUID = "932c32bd-0005-47a2-835a-a8d455b859dd";

    private static final String PHILIPS_DEVCONF_INFO_SERVICE_UUID = "0000fe0f-0000-1000-8000-00805f9b34fb";
    private static final String PHILIPS_USER_DEVNAME_CHARAC_UUID = "97fe6561-0003-4f62-86e9-b71ee2da3d22";

    private final String adapter, mac;

    public PhilipsHueBle(String adapter, String mac) {
        this.adapter = Objects.requireNonNull(adapter);
        this.mac = Objects.requireNonNull(mac);
    }

    public PhilipsHueBle(String mac) {
        this("hci0", mac);
    }

    @Override
    public PhilipsHueBle power(boolean on) {
        try {
            BluetoothGattCharacteristic api = getPrimaryServiceCharacteristic(PHILIPS_POWER_CHARAC_UUID);

            if (LOG.isDebugEnabled()) {
                BluetoothDevice device = api.getService().getDevice();
                LOG.debug("sending command {} to Philips Hue '{}' ({})",
                        "power(" + (on ? "on" : "off") + ")",
                        device.getName(),
                        device.getAddress()
                );
            }

            byte b = on ? (byte) 1 : (byte) 0;
            api.writeValue(new byte[]{b}, Collections.emptyMap());
        } catch (DBusException e) {
            throw new BluetoothException("unable to invoke command on Philips Hue: " + e, e);
        }

        return this;
    }

    @Override
    public PhilipsHueBle setBrightness(byte percent) {
        try {
            BluetoothGattCharacteristic api = getPrimaryServiceCharacteristic(PHILIPS_BRIGHTNESS_CHARAC_UUID);

            if (LOG.isDebugEnabled()) {
                BluetoothDevice device = api.getService().getDevice();
                LOG.debug("sending command {} to Philips Hue '{}' ({})",
                        "setBrightness(" + percent + ")",
                        device.getName(),
                        device.getAddress()
                );
            }

            api.writeValue(new byte[]{percent}, Collections.emptyMap());
        } catch (DBusException e) {
            throw new BluetoothException("unable to invoke command on Philips Hue: " + e, e);
        }

        return this;
    }

    @Override
    public PhilipsHueBle setTemperature(int kelvin) {
        // TODO fixme
//        try {
//            BluetoothGattCharacteristic api = getPrimaryServiceCharacteristic(PHILIPS_TEMPERATURE_CHARAC_UUID);
//
//            if (LOG.isDebugEnabled()) {
//                BluetoothDevice device = api.getService().getDevice();
//                LOG.debug("sending command {} to Philips Hue '{}' ({})",
//                        "setTemperature(" + kelvin + ")",
//                        device.getName(),
//                        device.getAddress()
//                );
//            }
//
//            api.writeValue(new byte[]{(byte) (kelvin >> 8), (byte) kelvin}, Collections.emptyMap());
//        } catch (DBusException e) {
//            throw new BluetoothException("unable to invoke command on Philips Hue: " + e, e);
//        }

        return this;
    }

    @Override
    public PhilipsHueBle setScene(byte scene) throws UnsupportedOperationException {
        // TODO implement
        LOG.info("not supported invocation silently ignored: setScene");
        return this;
    }

    @Override
    public PhilipsHueBle setColor(int red, int green, int blue) {
        // TODO implement
        LOG.info("not supported invocation silently ignored: setColor");
        return this;
    }

    private BluetoothGattCharacteristic getPrimaryServiceCharacteristic(String characId) {
        // TODO maintain cache of characs ?
//        Map<DiscoveryFilter, Object> filter = new HashMap<>();
//        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
//        filter.put(DiscoveryFilter.UUIDs, new String[]{PHILIPS_PRIMARY_SERVICE_UUID});
//
//        BluetoothGattCharacteristic characteristic = retrieveCharacteristic(
//                this.adapter,
//                this.mac,
//                PHILIPS_PRIMARY_SERVICE_UUID,
//                characId,
//                filter);
//
//        BluetoothHelper.reconnectIfNeeded(characteristic);
//        return characteristic;
        return null;
    }
}
