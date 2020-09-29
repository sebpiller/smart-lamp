package ch.sebpiller.iot.bluetooth.luke.roberts.lamp.f;

import ch.sebpiller.iot.bluetooth.BluetoothHelper;
import ch.sebpiller.iot.bluetooth.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.lamp.AbstractLampBase;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;
import static ch.sebpiller.iot.bluetooth.BluetoothHelper.findDeviceOnAdapter;

/**
 * Implementation of a {@link SmartLampFacade} able to drive a "Luke Roberts' model Lamp F" with bluetooth BLE.
 */
public class LukeRobertsLampF extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(LukeRobertsLampF.class);

    private final LukeRoberts.LampF.Config config;
    private Map<DiscoveryFilter, Object> filter;

    /**
     * The Bluetooth endpoint to invoke to control the lamp.
     */
    private BluetoothGattCharacteristic externalApi;

    public LukeRobertsLampF() {
        this(LukeRoberts.LampF.Config.getDefaultConfig());
    }

    public LukeRobertsLampF(LukeRoberts.LampF.Config config) {
        this.config = Objects.requireNonNull(config);

        filter = new HashMap<>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.AUTO);
        filter.put(DiscoveryFilter.UUIDs, new String[]{
                config.getCustomControlService().getUserExternalApiEndpoint().getUuid()
        });
    }

    public LukeRobertsLampF(LukeRoberts.LampF.Config config, Map<DiscoveryFilter, Object> filter) {
        this(config);
        this.filter = Objects.requireNonNull(filter);
    }

    private BluetoothGattCharacteristic getExternalApi() {
        if (externalApi == null) {
            externalApi = discoverExternalApiEndpoint(discoverDeviceManager(), config, filter);
        }

        return externalApi;
    }

    private BluetoothGattCharacteristic discoverExternalApiEndpoint(DeviceManager manager, LukeRoberts.LampF.Config
            config, Map<DiscoveryFilter, Object> filter) {
        try {
            return tryDiscoverExternalApiEndpoint(manager, config, filter);
        } catch (DBusException e) {
            throw new IllegalStateException("unable to find external api endpoint: " + e, e);
        }
    }

    private BluetoothGattCharacteristic tryDiscoverExternalApiEndpoint(DeviceManager manager, LukeRoberts.LampF.Config config, Map<DiscoveryFilter, Object> filter) throws DBusException {
        // TODO find out if the filter is really useful here
        if (filter != null && !filter.isEmpty()) {
            manager.setScanFilter(filter);
        }

        BluetoothDevice lampF = findDeviceOnAdapter(manager, config.getLocalBtAdapter(), config.getMac());

        LukeRoberts.LampF.Config.CustomControlService ccSer = config.getCustomControlService();
        BluetoothGattService customControlService = Objects.requireNonNull(
                lampF.getGattServiceByUuid(ccSer.getUuid()), "unable to connect to the custom control service " + ccSer.getUuid() + ": maybe the lamp is out of range."
        );
        LOG.info("found GATT custom control service {} at UUID {}", customControlService, ccSer.getUuid());

        String externalApiUuid = ccSer.getUserExternalApiEndpoint().getUuid();
        BluetoothGattCharacteristic api = Objects.requireNonNull(
                customControlService.getGattCharacteristicByUuid(externalApiUuid),
                "unable to connect to the external api " + externalApiUuid
        );
        LOG.info("external api {} found at characteristics UUID {}", api, externalApiUuid);

        if (LOG.isDebugEnabled()) {
            LOG.debug("  > inner structure: {}", ReflectionToStringBuilder.reflectionToString(api));
        }

        return api;
    }

    private void sendCommandToExternalApi(LukeRoberts.LampF.Command command, Byte... parameters) {
        reconnectIfNeeded();

        try {
            BluetoothGattCharacteristic api = getExternalApi();
            BluetoothDevice device = api.getService().getDevice();
            LOG.debug("sending command {} to Lamp F '{}' ({})",
                    command,
                    device.getName(),
                    device.getAddress()
            );

            api.writeValue(command.toByteArray(parameters), Collections.emptyMap());
        } catch (DBusException e) {
            throw new IllegalStateException("unable to invoke command on Lamp F: " + e, e);
        }
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

    private void reconnectIfNeeded() {
        BluetoothHelper.reconnectIfNeeded(getExternalApi());
    }


    public void selectScene(byte sceneId) {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.SELECT_SCENE, sceneId);
    }

    @Override
    public LukeRobertsLampF setScene(byte sceneId) {
        selectScene(sceneId);
        return this;
    }


    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SmartLampFacade setBrightness(byte percent) {
        Validate.inclusiveBetween(0, 100, percent, "percentage must be in range 0..100");
        sendCommandToExternalApi(LukeRoberts.LampF.Command.BRIGHTNESS, percent);
        return this;
    }

    @Override
    public SmartLampFacade setTemperature(int kelvin) {
        // 2700K..4000K no exception on invalid value here
        int k = Math.max(2700, Math.min(4000, kelvin));
        sendCommandToExternalApi(LukeRoberts.LampF.Command.COLOR_TEMP, (byte) (k >> 8), (byte) (k));
        return this;
    }

    @Override
    public SmartLampFacade power(boolean on) {
        selectScene(on ?
                LukeRoberts.LampF.LukeRobertsScene.DEFAULT_SCENE :
                LukeRoberts.LampF.LukeRobertsScene.SHUTDOWN_SCENE
        );

        return this;
    }

    public void pingV1() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V1);
    }

    public void pingV2() {
        sendCommandToExternalApi(LukeRoberts.LampF.Command.PING_V2);
    }


    @Override
    public void close() {
        try {
            BluetoothDevice device = getExternalApi().getService().getDevice();

            if (LOG.isDebugEnabled() && !device.isConnected()) {
                LOG.debug("the device was not connected");
            }

            boolean b = device.disconnect();

            if (LOG.isDebugEnabled() && !b) {
                LOG.debug("was unable to disconnect");
            }
        } finally {
            super.close();
        }
    }
}

