package ch.sebpiller.iot.bluetooth.luke.roberts.lamp.f;

import ch.sebpiller.iot.bluetooth.luke.roberts.LukeRoberts;

import ch.sebpiller.iot.lamp.SmartLampCli;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.*;

@Deprecated
public class Cli {
    private static final String SYSPROP_CONFIG = "config";
    private static final Logger LOG = LoggerFactory.getLogger(Cli.class);

    public static void main(String[] args) {
        LukeRoberts.LampF.Config config = LukeRoberts.LampF.Config.getDefaultConfig();

        if (System.getProperty("config") != null) {
            config = LukeRoberts.LampF.Config.loadOverriddenConfigFromSysprop(SYSPROP_CONFIG);
        }

        if (System.getProperty("lamp.f.mac") != null) {
            config.setMac(System.getProperty("lamp.f.mac"));
        }

        LukeRobertsLampF smartLamp = new LukeRobertsLampF(config);

        boolean demo = args.length > 0 && "demo".equals(args[0]);

        if (demo) {
            try {
                runDemo(smartLamp);
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("oups ! " + e.getMessage(), e);
            }
        } else {
            new SmartLampCli(smartLamp).run();
        }
    }

    // @deprecated not production code, just playing with stuffs here
    @Deprecated
    private static void runDemo(SmartLampFacade smartLamp) throws InterruptedException, ExecutionException {
        // we may fade both temperature AND brightness at the same time with this executor
        ExecutorService executor = Executors.newFixedThreadPool(2);

        smartLamp.power(false).sleep(2500).power(true).sleep(5000).power(false).sleep(2500);

        // blink a few times
        for (int i = 0; i < 20; i++) {
            smartLamp.power(true).sleep(50).power(false).sleep(100);
        }

        // fade using different styles
        smartLamp
                .fadeBrightnessFromTo((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.NORMAL).get()
                .fadeBrightnessFromTo((byte) 100, (byte) 0, SmartLampFacade.FadeStyle.FAST).get()
                .fadeBrightnessFromTo((byte) 0, (byte) 100, SmartLampFacade.FadeStyle.FAST).get()
                .fadeBrightnessFromTo((byte) 100, (byte) 0, SmartLampFacade.FadeStyle.NORMAL).get()
        ;

        SmartLampFacade.FadeStyle[] values = SmartLampFacade.FadeStyle.values();
        for (int i = 0; i < 5; i++) {
            SmartLampFacade.FadeStyle fadeStyle = values[i++ % values.length];

            // parallel execution sample. TODO does it work ?
            executor.execute(new FutureTask<>(() -> smartLamp.fadeBrightnessTo((byte) 100, fadeStyle).get()));
            executor.execute(new FutureTask<>(() -> smartLamp.fadeTemperatureTo((byte) 2700, fadeStyle).get()));
            executor.awaitTermination(10, TimeUnit.SECONDS);
            smartLamp.sleep(200);

            // parallel fade down to temp/bright
            executor.execute(new FutureTask<>(() -> smartLamp.fadeTemperatureTo((byte) 4000, fadeStyle).get()));
            executor.execute(new FutureTask<>(() -> smartLamp.fadeBrightnessTo((byte) 0, fadeStyle).get()));
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        // And now, just change for ever to random values...
        Random random = new Random();
        while (true) {
            smartLamp
                    .setBrightness((byte) random.nextInt(101))
                    .sleep(500)
                    .setTemperature(random.nextInt(6000))
                    .sleep(500)
            ;
        }
    }
}
