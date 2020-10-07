package ch.sebpiller.iot.bluetooth.philipps.hue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("ignore unless you have a real Philips Hue bulb connected to your system")
public class PhilipsHueBleTest {
    static String mac = "D7:0E:87:C6:D2:58";
    private PhilipsHueBle test;

    @Before
    public void setUp() throws Exception {
        test = new PhilipsHueBle("hci0", mac);
    }

    @After
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