package ch.sebpiller.iot.lamp.impl;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract Lamp implementation.
 * <p>
 * Implements the threading needed in order to provide "fading" behavior: change the value of the brightness,
 * temperature or color from one value to another.
 * <p>
 * IMPLEMENTATION NOTE: at the moment, I'm not able to retrieve the brightness/temperature/color of the lamp on startup.
 * Default values are assumed until the first call to set them. And due to the protocol used by most lamps, no
 * guarantee are made to the fact a value has been correctly applied (eg. Lamp F does not produce any error when you
 * try to apply a temperature of 10'000K).
 * <p>
 * Implements auto-closeable to enable try-with-resource programming idiom.
 */
public abstract class AbstractLampBase implements SmartLampFacade, AutoCloseable {
    /**
     * Single threaded executor to run fading behaviors ({@link #fadeBrightnessTo(byte, ch.sebpiller.iot.lamp.SmartLampFacade.FadeStyle)} etc)
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // locks to prevent multiple concurrent fading effects of the same property
    private final Lock tempLock = new ReentrantLock(), brightLock = new ReentrantLock(), colorLock = new ReentrantLock();

    // TODO load actual state of the lamp if possible...
    /**
     * local cache of the last temperature set
     */
    private int temperature = 4000;
    /**
     * local cache of the last brightness set
     */
    private byte brightness = 100;
    /**
     * local cache of the current color. assume white by default
     */
    private int[] color = new int[]{0xff, 0xff, 0xff};

    @Override
    public Future<AbstractLampBase> fadeBrightnessFromTo(byte from, byte to, SmartLampFacade.FadeStyle fadeStyle) {
        Validate.inclusiveBetween(0, 100, from, "percentage not in range 0..100");
        Validate.inclusiveBetween(0, 100, to, "percentage not in range 0..100");

        Callable<AbstractLampBase> callable = () -> {
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

            this.brightLock.lockInterruptibly();
            try {
                for (byte b = from; up ? b <= to : b >= to; b += inc) {
                    setBrightness(b);
                    this.brightness = b;

                    if (sleep > 0) {
                        sleep(sleep);
                    }
                }
            } finally {
                this.brightLock.unlock();
            }

            return AbstractLampBase.this;
        };

        return this.executor.submit(callable);
    }

    @Override
    public Future<AbstractLampBase> fadeBrightnessTo(byte percent, SmartLampFacade.FadeStyle fadeStyle) {
        return fadeBrightnessFromTo(this.brightness, percent, fadeStyle);
    }

    @Override
    public Future<AbstractLampBase> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle) {
        return fadeTemperatureFromTo(this.temperature, kelvin, fadeStyle);
    }

    @Override
    public Future<AbstractLampBase> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle) {
        Validate.inclusiveBetween(2000, 4000, from, "temperature not in range 2000..4000");
        Validate.inclusiveBetween(2000, 4000, to, "temperature not in range 2000..4000");

        Callable<AbstractLampBase> callable = () -> {
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

            this.tempLock.lockInterruptibly();
            try {
                for (int b = from; up ? b <= to : b >= to; b += inc) {
                    setTemperature(b);
                    this.temperature = b;

                    if (sleep > 0) {
                        sleep(sleep);
                    }
                }
            } finally {
                this.tempLock.unlock();
            }

            return AbstractLampBase.this;
        };

        return this.executor.submit(callable);
    }


    private int range0_255(int i) {
        return Math.min(Math.max(0, i), 255);
    }

    @Override
    public Future<AbstractLampBase> fadeColorFromTo(int[] f, int[] t, SmartLampFacade.FadeStyle fadeStyle) {
        String msg = "color must contain 3 integers in range 0..255 representing an RGB value";
        Validate.isTrue(f != null && f.length == 3, "from " + msg);
        Validate.isTrue(t != null && t.length == 3, "to " + msg);

        // clone mutable parameters to prevent concurrent modification, and force all values in range 0..255
        final int[] from = new int[]{range0_255(f[0]), range0_255(f[1]), range0_255(f[2])};
        final int[] to = new int[]{range0_255(t[0]), range0_255(t[1]), range0_255(t[2])};

        Callable<AbstractLampBase> callable = () -> {
            int iter = 50, sleep = 0;

            switch (fadeStyle) {
                case FAST:
                    iter /= 2;
                    break;
                case SLOW:
                    sleep = 50;
                    break;
            }

            this.colorLock.lockInterruptibly();
            try {
                int[] actual = new int[3];

                for (int i = 0; i < iter; i++) {
                    // compute actual color for each component, linear
                    actual[0] = Math.round(from[0] + (i * (to[0] - from[0]) / (float) iter));
                    actual[1] = Math.round(from[1] + (i * (to[1] - from[1]) / (float) iter));
                    actual[2] = Math.round(from[2] + (i * (to[2] - from[2]) / (float) iter));
                    setColor(actual[0], actual[1], actual[2]);

                    if (sleep > 0) {
                        sleep(sleep);
                    }
                }

                setColor(to[0], to[1], to[2]);
                this.color = to;
            } finally {
                this.colorLock.unlock();
            }

            return AbstractLampBase.this;
        };

        return this.executor.submit(callable);
    }

    @Override
    public Future<AbstractLampBase> fadeColorTo(int[] to, FadeStyle fadeStyle) {
        return fadeColorFromTo(this.color, to, fadeStyle);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Stop the executor used to schedule fading effects.
     */
    @Override
    public void close() throws Exception  {
        this.executor.shutdownNow();
    }
}
