package ch.sebpiller.iot.bluetooth.luke.roberts.lamp.f;

import ch.sebpiller.iot.bluetooth.lamp.AbstractBluetoothLamp;
import ch.sebpiller.iot.bluetooth.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;

/**
 * Implementation of a {@link SmartLampFacade} able to drive a "Luke Roberts' model Lamp F" with bluetooth BLE.
 */
public class LampFBle extends AbstractBluetoothLamp {
    private static final Logger LOG = LoggerFactory.getLogger(LampFBle.class);

    private final LukeRoberts.LampF.Config config;
    private Map<DiscoveryFilter, Object> filter;

    /**
     * The Bluetooth endpoint to invoke to control the lamp.
     */
    private BluetoothGattCharacteristic externalApi;

    public LampFBle() {
        this(LukeRoberts.LampF.Config.getDefaultConfig());
    }

    public LampFBle(LukeRoberts.LampF.Config config) {
        this.config = Objects.requireNonNull(config);

        filter = new HashMap<>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.AUTO);
        filter.put(DiscoveryFilter.UUIDs, new String[]{
                config.getCustomControlService().getUserExternalApiEndpoint().getUuid()
        });
    }

    public LampFBle(LukeRoberts.LampF.Config config, Map<DiscoveryFilter, Object> filter) {
        this(config);
        this.filter = Objects.requireNonNull(filter);
    }

    private void sendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        reconnectIfNeeded();

        try {
            BluetoothGattCharacteristic api = getExternalApi();
            if (LOG.isDebugEnabled()) {
                BluetoothDevice device = api.getService().getDevice();
                LOG.debug("sending command {} to Lamp F '{}' ({})",
                        command,
                        device.getName(),
                        device.getAddress()
                );
            }

            api.writeValue(command.toByteArray(parameters), Collections.emptyMap());
        } catch (DBusException e) {
            throw new IllegalStateException("unable to invoke command " + command + " on Lamp F: " + e, e);
        } catch (DBusExecutionException e) {
            throw new IllegalStateException("unable to invoke command " + command + " on Lamp F: " +
                    "error type is " + e.getType() + ": " + e, e);
        }
    }

    private BluetoothGattCharacteristic getExternalApi() {
        if (externalApi == null) {
            externalApi = discoverBluetoothCharacteristic(
                    discoverDeviceManager(),
                    config.getLocalBtAdapter(),
                    config.getMac(),
                    config.getCustomControlService().getUuid(),
                    config.getCustomControlService().getUserExternalApiEndpoint().getUuid(),
                    filter
            );
        }

        return externalApi;
    }

    public byte[] readValueFromExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        reconnectIfNeeded();

        try {
            Map<String, Object> options = new HashMap<>();

            byte[] reversed = command.toByteArray(parameters);
            ArrayUtils.reverse(reversed);

            BluetoothGattCharacteristic api = getExternalApi();

            BluetoothDevice device = api.getService().getDevice();
            LOG.debug("{}: reading value from Lamp F '{}' ({})",
                    command,
                    device.getName(),
                    device.getAddress()
            );

            byte[] data = api.readValue(options);
            return data;
        } catch (DBusException e) {
            throw new IllegalStateException("unable to invoke command on Lamp F: " + e, e);
        }
    }

    public void selectScene(byte sceneId) {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.SELECT_SCENE, sceneId);
    }

    @Override
    public LampFBle setScene(byte sceneId) {
        selectScene(sceneId);
        return this;
    }

    @Override
    public LampFBle setBrightness(byte percent) {
        Validate.inclusiveBetween(0, 100, percent, "percentage must be in range 0..100");
        sendCommandToExternalApi(LukeRoberts.LampF.Command.BRIGHTNESS, percent);
        return this;
    }

    @Override
    public LampFBle setTemperature(int kelvin) {
        // 2700K..4000K no exception on invalid value here
        int k = Math.max(2700, Math.min(4000, kelvin));
        sendCommandToExternalApi(LukeRoberts.LampF.Command.COLOR_TEMP, (byte) (k >> 8), (byte) (k));
        return this;
    }

    @Override
    public LampFBle power(boolean on) {
        selectScene(on ?
                LukeRoberts.LampF.Scene.DEFAULT_SCENE.getId() :
                LukeRoberts.LampF.Scene.SHUTDOWN_SCENE.getId()
        );

        return this;
    }

    public void pingV1() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V1);
    }

    public void pingV2() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V2);
    }

    /**
     * @param hue Color.
     */
    public void immediateLight(int duration, int sat, int hue, int temp, int mtemp, int mbrightness) {
        // FIXME improve code
        System.out.println(String.format("setting to duration %s, sat %s, hue %s, temp %s, mtemp %s, mbright %s",
                duration, sat, hue, temp, mtemp, mbrightness
        ));
        /* structure:
         * XX Flags that specify what content is present
         *
         * DDDD Duration in ms, 0 for infinite
         * SS Saturation 0 .. 255
         * HHHH Hue 0 .. 65535
         * HHHH Kelvin 2700 .. 4000 for white light when SS = 0
         *
         * KKKK Kelvin 2700 .. 4000
         * BB Brightness 0 .. 255
         */
        List<Byte> bytes = new ArrayList<>(12);
        byte xx = 0x00;

        bytes.add((byte) (duration >> 8));
        bytes.add((byte) (duration));

        ////////
        if (true) {
            xx |= 0x01;
            int i = sat == 0 ? hue : temp;
            bytes.add((byte) sat);
            bytes.add((byte) (i >> 8));
            bytes.add((byte) (i));
            bytes.add((byte) (mbrightness));
        }

        ////////
        if (true) {
            xx |= 0x02;
            bytes.add((byte) (mtemp >> 8));
            bytes.add((byte) (mtemp));
            bytes.add((byte) (mbrightness));
        }

        bytes.add(0, xx);

        sendCommandToExternalApi(LukeRoberts.LampF.Command.IMMEDIATE_LIGHT, bytes.toArray(new Byte[bytes.size()]));
    }

    @Override
    public void close() {
        try {
            externalApi = null;
        } finally {
            super.close();
        }
    }
}

