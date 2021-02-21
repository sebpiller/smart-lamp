package ch.sebpiller.iot.bluetooth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BluetoothHelperTest {

    @Disabled("fails on windows")
    @Test
    public void printBluetoothEnvironment() {
        BluetoothHelper.printBluetoothEnvironment();
    }
}