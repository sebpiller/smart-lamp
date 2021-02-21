package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;

import ch.sebpiller.iot.bluetooth.BluetoothHelper;
import ch.sebpiller.iot.bluetooth.lamp.AbstractBluetoothLamp;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import org.apache.commons.lang3.ArrayUtils;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

/**
 * Implementation of a {@link SmartLampFacade} able to drive a "Luke Roberts' model Lamp F" with bluetooth BLE.
 */
public class LampFBle extends AbstractBluetoothLamp {
    private static final Logger LOG = LoggerFactory.getLogger(LampFBle.class);

    private final LukeRoberts.LampF.Config config;

    @Override
    public LampFBle sleep(int millis) {
        super.sleep(millis);
        return this;
    }

    /**
     * The Bluetooth endpoint to invoke to control the lamp.
     */
    private BluetoothGattCharacteristic externalApi;

    public LampFBle() {
        this(LukeRoberts.LampF.Config.getDefaultConfig());
    }

    public LampFBle(LukeRoberts.LampF.Config config) {
        this.config = Objects.requireNonNull(config);
    }

    private void sendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        retry((Callable<Void>) () -> {
            trySendCommandToExternalApi(command, parameters);
            return null;
        }, 3, DBusException.class, DBusExecutionException.class);
    }

    private void trySendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte[] parameters) throws DBusException, DBusExecutionException {
        BluetoothGattCharacteristic api = getExternalApi();
        BluetoothHelper.reconnectIfNeeded(api);

        if (LOG.isTraceEnabled()) {
            BluetoothDevice device = api.getService().getDevice();
            LOG.trace("sending command {} to Lamp F '{}' ({})",
                    command,
                    device.getName(),
                    device.getAddress()
            );
        }

        api.writeValue(command.toByteArray(parameters), Collections.emptyMap());
    }

    private BluetoothGattCharacteristic getExternalApi() {
        if (this.externalApi == null) {
            Map<DiscoveryFilter, Object> filter = new HashMap<>();
            filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
            filter.put(DiscoveryFilter.UUIDs, new String[]{
                    this.config.getCustomControlService().getUserExternalApiEndpoint().getUuid()
            });

            this.externalApi = retrieveCharacteristic(
                    this.config.getLocalBtAdapter(),
                    this.config.getMac(),
                    this.config.getCustomControlService().getUuid(),
                    this.config.getCustomControlService().getUserExternalApiEndpoint().getUuid(),
                    filter
            );
        }

        return this.externalApi;
    }

    public byte[] readValueFromExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        try {
            // FIXME not working yet
            Map<String, Object> options = new HashMap<>();
            //options.put("offset", "");

            byte[] reversed = command.toByteArray(parameters);
            ArrayUtils.reverse(reversed);

            BluetoothGattCharacteristic api = getExternalApi();

            BluetoothDevice device = api.getService().getDevice();
            LOG.debug("{}: reading value from Lamp F '{}' ({})",
                    command,
                    device.getName(),
                    device.getAddress()
            );

            return api.readValue(options);
        } catch (DBusException e) {
            throw new IllegalStateException("unable to invoke command on Lamp F: " + e, e);
        }
    }

    public LampFBle selectScene(LukeRoberts.LampF.Scene scene) {
        setScene(scene.getId());
        return this;
    }

    @Override
    public LampFBle setScene(byte sceneId) {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.SELECT_SCENE, sceneId);
        return this;
    }

    public LampFBle adjustBrightness(byte percent) {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.RELATIVE_BRIGHTNESS, percent);
        return this;
    }

    @Override
    public LampFBle setBrightness(byte percent) {
        //Validate.inclusiveBetween(0, 100, percent, "percentage must be in range 0..100");
        sendCommandToExternalApi(LukeRoberts.LampF.Command.BRIGHTNESS, percent);
        return this;
    }

    @Override
    public LampFBle setTemperature(int kelvin) {
        // valid value are 2700K..4000K
        int k = min(max(2700, kelvin), 4000);
        sendCommandToExternalApi(LukeRoberts.LampF.Command.COLOR_TEMP, (byte) (k >> 8), (byte) (k));
        return this;
    }

    @Override
    public LampFBle power(boolean on) {
        selectScene(on ?
                LukeRoberts.LampF.Scene.DEFAULT_SCENE :
                LukeRoberts.LampF.Scene.SHUTDOWN_SCENE
        );
        return this;
    }

    @Override
    public LampFBle setColor(int red, int green, int blue) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setting lamp f color to #{}{}{}", format("%02X", red), format("%02X", green), format("%02X", blue));
        }
        int r = min(max(0x00, red), 0xFF);
        int g = min(max(0x00, green), 0xFF);
        int b = min(max(0x00, blue), 0xFF);

        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        immediateLight(0, Math.round(hsb[1] * 255f), Math.round(hsb[0] * 65_535f), 0, 0, 0);
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

        // TODO make correct use of immediate light body
        ////////
        if (true) {
            xx |= 0x01;
            bytes.add((byte) (duration >> 8));
            bytes.add((byte) (duration));

            int i = sat == 0 ? temp : hue;
            bytes.add((byte) sat);
            bytes.add((byte) (i >> 8));
            bytes.add((byte) (i));
        }

        ////////
        if (true) {
            xx |= 0x02;
            bytes.add((byte) (mtemp >> 8));
            bytes.add((byte) (mtemp));
            bytes.add((byte) (mbrightness));
        }

        bytes.add(0, xx);
        sendCommandToExternalApi(LukeRoberts.LampF.Command.IMMEDIATE_LIGHT, bytes.toArray(new Byte[0]));
    }
}

