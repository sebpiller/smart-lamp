package ch.sebpiller.iot.bluetooth;

import org.junit.Ignore;
import org.junit.Test;

public class BluetoothHelperTest {

    @Ignore("fails on windows")
    @Test
    public void printBluetoothEnvironment() {
        BluetoothHelper.printBluetoothEnvironment();
    }
}