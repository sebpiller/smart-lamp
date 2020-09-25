package ch.sebpiller.iot.lamp;

import java.util.concurrent.Future;

/**
 * Interface of a controller classes able to drive a Smart Lamp, using any kind of connectivity.
 * <p>
 * When possible, the methods returns a {@link SmartLampFacade} reference to allow easy chaining.
 */
public interface SmartLampFacade {
    /**
     * Utility method to facilitate the development of a timed sequence of actions.
     * <p>
     * WARNING: this is a rather inaccurate (but simple) way to time a sequence, if you need professional rendering,
     * then you need another type of sequencer.
     */
    default SmartLampFacade sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * Turn the lamp on/off.
     *
     * @return this facade for chaining.
     */
    SmartLampFacade power(boolean on);

    /**
     * Modify the brightness of the lamp.
     *
     * @param percent Percentage of brighness
     * @return this facade for chaining.
     * @throws UnsupportedOperationException if the device do not support the modification of the brightness.
     */
    SmartLampFacade setBrightness(byte percent) throws UnsupportedOperationException;

    /**
     * Change the brightness of the lamp from a point to another, if supported.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeBrightnessFromTo(byte from, byte to, FadeStyle fadeStyle);

    /**
     * Relative from where we are, going to #percent brightness.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeBrightnessTo(byte percent, FadeStyle fadeStyle);

    /**
     * Change the temperature of the lamp, if supported.
     */
    SmartLampFacade setTemperature(int kelvin);

    /**
     * Fade the temperature of the lamp from a point to another, if supported.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle);

    /**
     * Relative from where wh are, change the temperature of the lamp, if supported.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle);

    /**
     * Describes the different available styles to fade a value.
     */
    enum FadeStyle {
        FAST,
        NORMAL,
        SLOW;
    }
}
