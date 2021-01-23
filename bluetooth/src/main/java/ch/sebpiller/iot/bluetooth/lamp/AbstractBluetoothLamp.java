package ch.sebpiller.iot.bluetooth.lamp;

import ch.sebpiller.iot.bluetooth.BluetoothException;
import ch.sebpiller.iot.lamp.impl.AbstractLampBase;
import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import static ch.sebpiller.iot.bluetooth.BluetoothHelper.discoverDeviceManager;
import static ch.sebpiller.iot.bluetooth.BluetoothHelper.findDeviceOnAdapter;

/**
 * Base of a smart lamp that supports BLE connectivity (Bluetooth Low Energy).
 */
public abstract class AbstractBluetoothLamp extends AbstractLampBase {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBluetoothLamp.class);
    private BluetoothDevice device;

    protected final BluetoothGattCharacteristic retrieveCharacteristic(String adapter, String mac, String serviceUuid, String characUuid, Map<DiscoveryFilter, Object> filter) throws BluetoothException {
        try {
            BluetoothGattService service = getBluetoothDevice(adapter, mac, filter).getGattServiceByUuid(serviceUuid);
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

    protected final BluetoothDevice getBluetoothDevice(String adapter, String mac, Map<DiscoveryFilter, Object> filter) throws BluezInvalidArgumentsException, BluezNotReadyException, BluezNotSupportedException, BluezFailedException {
        DeviceManager manager = discoverDeviceManager();

        // TODO find out if the filter is really useful here
        if (filter != null && !filter.isEmpty()) {
            manager.setScanFilter(filter);
        }

        // *****************
        device = findDeviceOnAdapter(manager, adapter, mac);
        if (device == null || !device.connect()) {
            throw new BluetoothException("can not connect to device " + mac + "@" + adapter);
        }

        return device;
    }

    @Override
    public void close() {
        try {
            if (device != null) {
                device.disconnect();
            }
        } finally {
            super.close();
        }
    }


    /**
     * Retry to run a function a few times, retry if specific exceptions occur.
     *
     * @param timeoutExceptionClasses what exceptions should lead to retry. Default: any exception
     */
    public static <T> T retry(Callable<T> call, int maxRetries, Class<? extends Exception>... timeoutExceptionClasses) {
        timeoutExceptionClasses = timeoutExceptionClasses.length == 0 ? new Class[]{Exception.class} : timeoutExceptionClasses;
        int retryCounter = 0;
        Exception lastException = null;
        while (retryCounter < maxRetries) {
            try {
                return call.call();
            } catch (Exception e) {
                lastException = e;
                if (Arrays.stream(timeoutExceptionClasses).noneMatch(tClass ->
                        tClass.isAssignableFrom(e.getClass())
                ))
                    throw lastException instanceof RuntimeException ?
                            ((RuntimeException) lastException) :
                            new RuntimeException(lastException);
                else {
                    retryCounter++;
                    LOG.warn("FAILED - Command failed on retry {} of {}", retryCounter, maxRetries);
                    if (retryCounter >= maxRetries) {
                        break;
                    }
                }
            }
        }
        throw lastException instanceof RuntimeException ?
                ((RuntimeException) lastException) :
                new RuntimeException(lastException);
    }
}
