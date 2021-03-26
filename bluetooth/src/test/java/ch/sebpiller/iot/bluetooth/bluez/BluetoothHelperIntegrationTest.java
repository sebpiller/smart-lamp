package ch.sebpiller.iot.bluetooth.bluez;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@DisplayName("Bluetooth integration test")
@EnabledIfEnvironmentVariable(named = "HOSTNAME", matches = "jenkins.*")
class BluetoothHelperIntegrationTest {

    @Test
    @DisplayName("print bluetooth environment")
    void printBluetoothEnvironment() {
        BluetoothHelper.printBluetoothEnvironment();
    }

}