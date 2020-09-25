package ch.sebpiller.iot.lamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A fake lamp that only log the calls.
 */
public class LoggingLamp implements SmartLampFacade {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingLamp.class);
    private final CompletableFuture<LoggingLamp> now = new CompletableFuture<LoggingLamp>() {
        @Override
        public LoggingLamp get() {
            return LoggingLamp.this;
        }
    };

    @Override
    public SmartLampFacade power(boolean b) {
        LOG.info("power({})", b);
        return this;
    }

    @Override
    public SmartLampFacade setBrightness(byte b) throws UnsupportedOperationException {
        LOG.info("setBrightness({})", b);
        return this;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeBrightnessFromTo(byte b, byte b1, FadeStyle fadeStyle) {
        LOG.info("fadeToBrightness({}, {}, {})", b, b1, fadeStyle);
        return now;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeBrightnessTo(byte b, FadeStyle fadeStyle) {
        LOG.info("fadeBrightnessTo({}, {})", b, fadeStyle);
        return now;
    }

    @Override
    public SmartLampFacade setTemperature(int i) {
        LOG.info("setTemperature({})", i);
        return this;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeTemperatureFromTo(int i, int i1, FadeStyle fadeStyle) {
        LOG.info("fadeTemperatureFromTo({}, {}, {})", i, i1, fadeStyle);
        return now;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeTemperatureTo(int i, FadeStyle fadeStyle) {
        LOG.info("fadeTemperatureTo({}, {})", i, fadeStyle);
        return now;
    }
}
