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
     * WARNING: this is a rather inaccurate (but simple) way to time a sequence, if you need a more precise rendering,
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
     * @param percent Percentage of brightness
     * @return this facade for chaining.
     * @throws UnsupportedOperationException if the device does not support the modification of the brightness.
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
     *
     * @throws UnsupportedOperationException in case this lamp does not support the concept of temperature.
     */
    SmartLampFacade setTemperature(int kelvin) throws UnsupportedOperationException;

    /**
     * Fade the temperature of the lamp from a point to another, if supported.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle);

    /**
     * Relative from where we are, change the temperature of the lamp, if supported.
     *
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle);

    /**
     * Fade the color of the lamp from a value to another. The precise meaning of "fade a color" is implementation
     * dependant, and can make use of different color scheme (RGB, HSB, HSV, etc.)
     *
     * @param from      3 ints in range 0..255 containing an RGB color.
     * @param to        3 ints in range 0..255 containing an RGB color.
     * @param fadeStyle Style of fading.
     * @return this future facade for chaining.
     */
    Future<? extends SmartLampFacade> fadeColorFromTo(int[] from, int[] to, SmartLampFacade.FadeStyle fadeStyle);

    Future<? extends SmartLampFacade> fadeColorTo(int[] to, SmartLampFacade.FadeStyle fadeStyle);

    /**
     * For lamp supporting it, modify the scene actually in use.
     *
     * @throws UnsupportedOperationException in case this lamp does not support the concept of scene.
     */
    SmartLampFacade setScene(byte scene) throws UnsupportedOperationException;

    /**
     * Modify the color of the lamp (RGB).
     *
     * @param red   0..255
     * @param green 0..255
     * @param blue  0..255
     */
    SmartLampFacade setColor(int red, int green, int blue);

    /**
     * Describes the different available styles to fade a value. Implementations are free to interpret the meaning of
     * each value in any way they like.
     */
    enum FadeStyle {
        FAST,
        NORMAL,
        SLOW;
    }
}
