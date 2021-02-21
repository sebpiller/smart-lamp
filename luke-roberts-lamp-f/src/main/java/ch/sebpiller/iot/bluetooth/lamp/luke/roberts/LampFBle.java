package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;

import ch.sebpiller.iot.bluetooth.BluetoothDelegate;
import ch.sebpiller.iot.bluetooth.BluezDelegate;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.iot.lamp.impl.AbstractLampBase;
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

import static java.lang.Math.*;
import static java.lang.String.format;

/**
 * Implementation of a {@link SmartLampFacade} able to drive a "Luke Roberts' model Lamp F".
 */
public class LampFBle extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(LampFBle.class);

    private final BluetoothDelegate bluetoothDelegate;

    /**
     * Lamp minimum temperature in kelvin.
     */
    private static final int MIN_TEMP = 2700;
    /**
     * Lamp maximum temperature in kelvin.
     */
    private static final int MAX_TEMP = 4000;

    private final LukeRoberts.LampF.Config config;

    // values cached by call of #immediateLight
    private Byte _sat, _bri, _mbri;
    private Integer _hue, _temp, _mtemp;

    public LampFBle() {
        this(LukeRoberts.LampF.Config.getDefaultConfig());
    }

    /**
     * Default bluetooth implementation is BlueZ (does not work under windows !)
     */
    public LampFBle(LukeRoberts.LampF.Config config) {
        this(new BluezDelegate(
                config.getLocalBtAdapter(),
                config.getMac(),
                UUID.fromString(config.getCustomControlService().getUuid()),
                UUID.fromString(config.getCustomControlService().getUserExternalApiEndpoint().getUuid())
        ), config);
    }

    public LampFBle(BluetoothDelegate delegate, LukeRoberts.LampF.Config config) {
        this.bluetoothDelegate = Objects.requireNonNull(delegate);
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public LampFBle sleep(int millis) {
        super.sleep(millis);
        return this;
    }

    private void sendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        BluetoothDelegate.retry((Callable<Void>) () -> {
            trySendCommandToExternalApi(command, parameters);
            return null;
        }, 3, DBusException.class, DBusExecutionException.class);
    }

    private void trySendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte[] parameters) throws DBusException, DBusExecutionException {
        this.bluetoothDelegate.write(command.toByteArray(parameters));
    }


//    public byte[] readValueFromExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
//        try {
//            // FIXME not working yet
//            Map<String, Object> options = new HashMap<>();
//            //options.put("offset", "");
//
//            byte[] reversed = command.toByteArray(parameters);
//            ArrayUtils.reverse(reversed);
//
//            BluetoothGattCharacteristic api = getExternalApi();
//
//            BluetoothDevice device = api.getService().getDevice();
//            LOG.debug("{}: reading value from Lamp F '{}' ({})",
//                    command,
//                    device.getName(),
//                    device.getAddress()
//            );
//
//            return api.readValue(options);
//        } catch (DBusException e) {
//            throw new IllegalStateException("unable to invoke command on Lamp F: " + e, e);
//        }
//    }


    public LampFBle selectScene(LukeRoberts.LampF.Scene scene) {
        setScene(scene.getId());
        return this;
    }

    @Override
    public LampFBle setScene(byte sceneId) {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.SELECT_SCENE, sceneId);
        invalidateCacheFromImmediateLight();
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
        int k = lampTemp(kelvin);
        this._mtemp = k;
        sendCommandToExternalApi(LukeRoberts.LampF.Command.COLOR_TEMP, (byte) (k >> 8), (byte) (k));
        return this;
    }

    /**
     * Range the given kelvin to acceptable lamp temperature.
     */
    private int lampTemp(int kelvin) {
        return min(max(MIN_TEMP, kelvin), MAX_TEMP);
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
        immediateLight(0,
                round(hsb[0] * 65_535f), (byte) round(hsb[1] * 255f), (byte) round(hsb[2] * 255f), null,
                null, null);
        return this;
    }

    public void pingV1() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V1);
    }

    public void pingV2() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V2);
    }

    public void setTopTemperature(int kelvin) {
        int k = lampTemp(kelvin);
        immediateLight(0,
                null, (byte) 0, null, k,
                null, null);
    }

    /**
     * Invalidate cached values from the command {@link #immediateLight(int, Integer, Byte, Byte, Integer, Integer, Byte)}.
     */
    private void invalidateCacheFromImmediateLight() {
        this._sat = null;
        this._bri = null;
        this._mbri = null;
        this._hue = null;
        this._temp = null;
        this._mtemp = null;
    }

    /**
     * Sends the command "IMMEDIATE_LIGHT" to the Lamp.
     * This command can change either the configuration of the top bulb (the colored one), the main bulb
     * (with only color temperature and brightness), or both with a single call.
     * <p>
     * If all values #sat, #bri, #hue and #temp are null, then this call does not change any setting of the top bulb.
     * <p>
     * If all values #mtemp and #mbrightness are null, then this call does not change any setting of the main bulb.
     * <p>
     * NOTE: after creating an instance of {@link LampFBle}, all values are defaulted, even if the lamp actually has
     * different settings. If is advised to sync internal state with the lamp by calling this method once. Setting the
     * lamp's scene will actually invalidate all values.
     * <p>
     * NOTE: users of the API would rather use the methods {@link #setColor(int, int, int)},
     * {@link #setTopTemperature(int)}, etc than this low-level call.
     *
     * @param duration Duration in seconds to apply this configuration
     * @param hue      Top bulb HUE value in HSB color space. A null value means no change.
     * @param sat      Top bulb saturation in HSB color space. A null value means no changes.
     * @param bri      Top bulb brightness in HSB color space. A null value means no change.
     * @param temp     Top bulb temperature value (2700K..4000K). Apply only if saturation is 0 or null (eg.
     *                 when converting black color to HSB).
     * @param mtemp    Main bulb temperature value (2700K..4000K). A null value means no change.
     * @param mbri     Main bulb brightness. A null value means no change.
     */
    public void immediateLight(int duration, // duration apply to this command
                               Integer hue, Byte sat, Byte bri, Integer temp, // top bulb
                               Integer mtemp, Byte mbri // main bulb
    ) {
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

        // set duration
        bytes.add((byte) (duration >> 8));
        bytes.add((byte) (duration));

        ////////
        boolean changeTop = false;
        if (hue != null) {
            this._hue = hue;
            changeTop = true;
        }
        if (sat != null) {
            this._sat = sat;
            changeTop = true;
        }
        if (bri != null) {
            this._bri = bri;
            changeTop = true;
        }

        if ((sat == null || sat == 0) && temp != null) {
            this._temp = lampTemp(temp);
            changeTop = true;
        } else {
            this._temp = null;
        }

        if (changeTop) {
            xx |= 0x01;

            // FIXME simplify case where temp has not been defined before and sat is 0
            int i = this._sat == 0 ?
                    (this._temp == null ? this._temp = lampTemp(Integer.MAX_VALUE) : this._temp)
                    : this._hue;
            bytes.add(this._sat);
            bytes.add((byte) (i >> 8));
            bytes.add((byte) (i));
            bytes.add(this._bri);
        }

        ////////
        boolean changeMain = false;

        if (mtemp != null) {
            this._mtemp = lampTemp(mtemp);
            changeMain = true;
        }
        if (mbri != null) {
            this._mbri = mbri;
            changeMain = true;
        }

        if (changeMain) {
            xx |= 0x02;
            bytes.add((byte) (this._mtemp.intValue() >> 8));
            bytes.add((byte) (this._mtemp.intValue()));
            bytes.add(this._mbri);
        }

        bytes.add(0, xx);
        sendCommandToExternalApi(LukeRoberts.LampF.Command.IMMEDIATE_LIGHT, bytes.toArray(new Byte[bytes.size()]));
    }

    @Override
    public void close() throws Exception {
        try {
            if (this.bluetoothDelegate instanceof AutoCloseable) {
                this.bluetoothDelegate.close();
            }
        } finally {
            super.close();
        }
    }
}

