package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import com.github.hypfvieh.bluetooth.DiscoveryFilter;
import com.github.hypfvieh.bluetooth.DiscoveryTransport;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//@Ignore("ignored unless in real world testing with an adequate lamp running, and a human can verify it actually worked.")
public class LampFBleBrightnessIntegrationTest {
    private static LukeRoberts.LampF.Config lampConfigForTesting;
    private static LampFBle facade;

    @BeforeClass
    public static void beforeClass() {
        lampConfigForTesting = LukeRoberts.LampF.Config.loadFromStream(LampFBleBrightnessIntegrationTest.class.
                getResourceAsStream("/luke-roberts-lamp-f-tests.yaml"));
        LukeRoberts.LampF.Config merge = LukeRoberts.LampF.Config.getDefaultConfig().merge(lampConfigForTesting);

        Map<DiscoveryFilter, Object> filter = new HashMap<>();
        filter.put(DiscoveryFilter.Transport, DiscoveryTransport.LE);
        filter.put(DiscoveryFilter.UUIDs, new String[]{merge.getCustomControlService()
                .getUserExternalApiEndpoint().getUuid()});

        facade = new LampFBle(merge, filter);
    }

    @Test
    public void testPower() {
        facade.power(true);
        facade.power(false);
        facade.power(true);
        facade.power(false);
    }

    /**
     * disco-mode is a simple on/off loop, like a stroboscope.
     */
    @Test
    public void testDisco() {
        for (int i = 0; i < 250; i++) {
            facade.setBrightness((byte) (i % 2 == 0 ? 0 : 100));
        }
    }

    @Test
    public void testFadeInOut() {
        // go up and down one unit at a time
        boolean direction = true;

        byte b = 0;
        for (int i = 0; i < 500; i++) {
            if (direction) {
                if (b == 100) {
                    direction = false;
                } else {
                    b++;
                }
            } else {
                if (b == 0) {
                    direction = true;
                } else {
                    b--;
                }
            }

            facade.setBrightness(b);
        }
    }

    @Test
    public void testAllBrightnesses() {
        for (byte b = 0; b < 100; b++) {
            facade.setBrightness(b);
        }
    }

    @Test
    public void testBrightnessMax() {
        facade.setBrightness((byte) 100);
    }

    @Test
    public void testBrightnessMedium() {
        facade.setBrightness((byte) 50);
    }

    @Test
    public void testBrightnessMin() {
        facade.setBrightness((byte) 0);
    }

    @Test
    public void testFadeFromToBrightness() throws Exception {
        facade
                .fadeBrightnessFromTo((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.SLOW).get(15, TimeUnit.SECONDS)
                .fadeBrightnessFromTo((byte) 100, (byte) 0, SmartLampFacade.FadeStyle.SLOW).get(15, TimeUnit.SECONDS)
                .fadeBrightnessFromTo((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.NORMAL).get(5, TimeUnit.SECONDS)
                .fadeBrightnessFromTo((byte) 100, (byte) 0, SmartLampFacade.FadeStyle.NORMAL).get(5, TimeUnit.SECONDS)
                .fadeBrightnessFromTo((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.FAST).get(3, TimeUnit.SECONDS)
                .fadeBrightnessFromTo((byte) 100, (byte) 0, SmartLampFacade.FadeStyle.FAST).get(3, TimeUnit.SECONDS)
        ;
    }

}