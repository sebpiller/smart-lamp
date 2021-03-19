package ch.sebpiller.iot.bluetooth.bluez.philipps.hue;

import org.junit.jupiter.api.*;

@Disabled("ignore unless you have a real Philips Hue bulb connected to your system")
public class PhilipsHueBleTest {
    static String mac = "D7:0E:87:C6:D2:58";
    private PhilipsHueBle test;

    @BeforeEach
    public void setUp() throws Exception {
        test = new PhilipsHueBle("hci0", mac);
    }

    @AfterEach
    public void tearDown() throws Exception {
        test.close();
    }

    @Test
    public void test() {
        int i = 0;

        while (i++ < 5) {
            System.out.println("blink");
            test.power(true).power(false).sleep(1000);
        }
    }
}