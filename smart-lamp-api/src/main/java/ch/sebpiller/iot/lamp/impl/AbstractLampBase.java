package ch.sebpiller.iot.lamp.impl;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * IMPLEMENTATION NOTE: at the moment, I'm not able to retrieve the brightness/temperature of the lamp. Default values
 * are assumed until the first call to set them. And due to the protocol used by most lamps, no guarantee are made to
 * the fact a value has been correctly applied (eg. Lamp F does not produce any error when you try to apply a
 * temperature of 10'000K).
 */
public abstract class AbstractLampBase implements SmartLampFacade, AutoCloseable {
    /**
     * Single threaded executor to run asynchronous behavior ({@link #fadeBrightnessTo(byte, ch.sebpiller.iot.lamp.SmartLampFacade.FadeStyle)})
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Lock tempLock = new ReentrantLock(), brightLock = new ReentrantLock();

    // TODO load actual state of the lamp
    /**
     * local cache of the last temperature set
     */
    private int temperature = 4000;
    /**
     * local cache of the last brightness set
     */
    private byte brightness = 100;

    @Override
    public Future<SmartLampFacade> fadeBrightnessFromTo(byte from, byte to, SmartLampFacade.FadeStyle fadeStyle) {
        Validate.inclusiveBetween(0, 100, from, "percentage not in range 0..100");
        Validate.inclusiveBetween(0, 100, to, "percentage not in range 0..100");

        Callable<SmartLampFacade> callable = () -> {
            boolean up = from < to;
            int inc = up ? 1 : -1, sleep = 0;

            switch (fadeStyle) {
                case FAST:
                    inc *= 2;
                    break;
                case SLOW:
                    sleep = 50;
                    break;
            }

            brightLock.lockInterruptibly();
            try {
                for (byte b = from; up ? b <= to : b >= to; b += inc) {
                    setBrightness(b);
                    this.brightness = b;

                    if (sleep > 0) {
                        sleep(sleep);
                    }
                }
            } finally {
                brightLock.unlock();
            }

            return AbstractLampBase.this;
        };

        return executor.submit(callable);
    }

    @Override
    public Future<SmartLampFacade> fadeBrightnessTo(byte percent, SmartLampFacade.FadeStyle fadeStyle) {
        return fadeBrightnessFromTo(brightness, percent, fadeStyle);
    }

    @Override
    public Future<SmartLampFacade> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle) {
        Validate.inclusiveBetween(2000, 4000, from, "temperature not in range 2000..4000");
        Validate.inclusiveBetween(2000, 4000, to, "temperature not in range 2000..4000");

        Callable<SmartLampFacade> callable = () -> {
            boolean up = from < to;
            int inc = up ? 10 : -10, sleep = 0;

            switch (fadeStyle) {
                case FAST:
                    inc *= 2;
                    break;
                case SLOW:
                    sleep = 50;
                    break;
            }

            tempLock.lockInterruptibly();
            try {
                for (int b = from; up ? b <= to : b >= to; b += inc) {
                    setTemperature(b);
                    this.temperature = b;

                    if (sleep > 0) {
                        sleep(sleep);
                    }
                }
            } finally {
                tempLock.unlock();
            }

            return AbstractLampBase.this;
        };

        return executor.submit(callable);
    }

    @Override
    public Future<SmartLampFacade> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle) {
        return fadeTemperatureFromTo(temperature, kelvin, fadeStyle);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stop the executor used to schedule fading effects.
     */
    @Override
    public void close() {
        executor.shutdownNow();
    }
}
