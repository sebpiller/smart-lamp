package ch.sebpiller.iot.lamp.impl;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A fake lamp that only logs the calls.
 */
public class LoggingLamp implements SmartLampFacade {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingLamp.class);
    private final Future<LoggingLamp> now = new CompletableFuture<LoggingLamp>() {
        @Override
        public LoggingLamp get() {
            return LoggingLamp.this;
        }
    };

    @Override
    public LoggingLamp power(boolean b) {
        LOG.info("power({})", b);
        return this;
    }

    @Override
    public LoggingLamp sleep(int millis) {
        LOG.info("sleep({})", millis);
        return this;
    }

    @Override
    public LoggingLamp setBrightness(byte b) {
        LOG.info("setBrightness({})", b);
        return this;
    }

    @Override
    public Future<LoggingLamp> fadeBrightnessFromTo(byte b, byte b1, FadeStyle fadeStyle) {
        LOG.info("fadeBrightnessFromTo({}, {}, {})", b, b1, fadeStyle);
        return now;
    }

    @Override
    public Future<LoggingLamp> fadeBrightnessTo(byte b, FadeStyle fadeStyle) {
        LOG.info("fadeBrightnessTo({}, {})", b, fadeStyle);
        return now;
    }

    @Override
    public LoggingLamp setTemperature(int i) {
        LOG.info("setTemperature({})", i);
        return this;
    }

    @Override
    public Future<LoggingLamp> fadeTemperatureFromTo(int i, int i1, FadeStyle fadeStyle) {
        LOG.info("fadeTemperatureFromTo({}, {}, {})", i, i1, fadeStyle);
        return now;
    }

    @Override
    public Future<LoggingLamp> fadeTemperatureTo(int i, FadeStyle fadeStyle) {
        LOG.info("fadeTemperatureTo({}, {})", i, fadeStyle);
        return now;
    }

    @Override
    public Future<LoggingLamp> fadeColorFromTo(int[] from, int[] to, FadeStyle fadeStyle) {
        LOG.info("fadeColorFromTo({}, {}, {})", from, to, fadeStyle);
        return now;
    }

    @Override
    public Future<LoggingLamp> fadeColorTo(int[] to, FadeStyle fadeStyle) {
        LOG.info("fadeColorTo({}, {})", to, fadeStyle);
        return now;
    }

    @Override
    public LoggingLamp setScene(byte scene) {
        LOG.info("setScene({})", scene);
        return this;
    }

    @Override
    public SmartLampFacade setColor(int red, int green, int blue) {
        LOG.info("setColor({},{},{})", red, green, blue);
        return this;
    }
}
