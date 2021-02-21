package ch.sebpiller.iot.bluetooth.scan;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothDevice;

@FunctionalInterface
public interface ScanHandler {
    void handle(BluetoothDevice device, ScanData data);
}