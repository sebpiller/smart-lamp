package ch.sebpiller.iot.bluetooth.philipps.hue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("ignore unless you have a real Philips Hue bulb connected to your system")
public class PhilipsHueBleTest {
    static String mac = "D7:0E:87:C6:D2:58";
    private PhilipsHueBle test;

    @BeforeAll
    public void setUp() throws Exception {
        test = new PhilipsHueBle("hci0", mac);
    }

    @AfterAll
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