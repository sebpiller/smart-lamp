package ch.sebpiller.iot.bluetooth.bluez.scan;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

@FunctionalInterface
public interface ScanHandler {
    void handle(BluetoothDevice device, ScanData data);
}