package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

//@Ignore("ignored unless in real world testing with an adequate lamp running, and a human can verify " +
//        "it actually worked.")
public class LampFBleIntegrationTest {
    private static LampFBle facade;

    @BeforeClass
    public static void beforeClass() {
        LukeRoberts.LampF.Config intgConfig = LukeRoberts.LampF.Config.loadFromStream(LampFBleIntegrationTest.class.
                getResourceAsStream("/luke-roberts-lamp-f-tests.yaml"));
        LukeRoberts.LampF.Config merge = LukeRoberts.LampF.Config
                .getDefaultConfig()
                .merge(intgConfig);
        facade = new LampFBle(merge);
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

    @Test
    public void testSelectScene() {
        facade.selectScene(LukeRoberts.LampF.Scene.SHUTDOWN_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.DEFAULT_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.BRIGHT_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.HIGHLIGHTS_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.WELCOME_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.CANDLE_LIGHT_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.SHINY_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.READING_SCENE);
        facade.selectScene(LukeRoberts.LampF.Scene.INDIRECT_SCENE);
    }


    @Test
    public void testChangeTemperatureWithImmediateLight() {
        // for tests of color changes, the best is to use an indirect scene.
        facade.selectScene(LukeRoberts.LampF.Scene.INDIRECT_SCENE);

        System.out.println("TESTING TEMPERATURE...");
        for (int j = 2700; j <= 4000; j += 10) {
            System.out.println("TEMP IS " + j);
            facade.immediateLight(0, 0, 0, j, 0, 0);
        }
        facade.sleep(2500);
    }

    @Test
    public void testChangeColorWithImmediateLight() {
        // for tests of color changes, the best is to use an indirect scene.
        facade.selectScene(LukeRoberts.LampF.Scene.INDIRECT_SCENE);

        // playing with saturation & hue of immediate light (changes the color of the light)
        System.out.println("TESTING SATURATION AND HUE...");
        for (int i = 255; i > 0; i -= 5) {
            for (int j = 0; j < 65535; j += 1000) {
                System.out.println("SATURATION IS " + i + ", HUE IS " + j);
                facade.immediateLight(0, i, j, 0, 0, 0);
            }
        }
    }

    @Test
    public void testSetColor() {
        // for tests of color changes, the best is to use an indirect scene.
        facade.selectScene(LukeRoberts.LampF.Scene.INDIRECT_SCENE);
        facade.sleep(2000);

        for (int i = 0; i < 10; i++) {
            facade
                    .setColor(0xFF, 0x00,  0x00).sleep(2000)
                    .setColor( 0x00,  0xFF,  0x00).sleep(2000)
                    .setColor(0x00, 0x00, 0xFF).sleep(2000)
            ;
        }
    }


}