package ch.sebpiller.iot.bluetooth.bluez;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BluetoothHelperTest {

    @Disabled("fails on windows")
    @Test
    public void printBluetoothEnvironment() {
        BluetoothHelper.printBluetoothEnvironment();
    }
}