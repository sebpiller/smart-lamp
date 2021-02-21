package ch.sebpiller.iot.lamp.impl;

import ch.sebpiller.iot.lamp.SmartLampFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Control several lamps at the same time, and asynchronously.
 */
// FIXME fails at the moment
public final class CompositeLampFacade implements SmartLampFacade {
    public final CompletableFuture<CompositeLampFacade> futureMe = new CompletableFuture<CompositeLampFacade>() {
        @Override
        public CompositeLampFacade get() {
            return CompositeLampFacade.this;
        }
    };

    private final List<SmartLampFacade> composites = Collections.synchronizedList(new ArrayList<>());

    public CompositeLampFacade(SmartLampFacade... composites) {
        this.composites.addAll(Arrays.asList(composites));
    }

    public static CompositeLampFacade from(SmartLampFacade... composites) {
        return new CompositeLampFacade(composites);
    }

    @Override
    public CompositeLampFacade power(boolean on) {
        composites.parallelStream().forEach((smartLampFacade) -> smartLampFacade.power(on));
        return this;
    }

    @Override
    public CompositeLampFacade setBrightness(byte percent) throws UnsupportedOperationException {
        composites.parallelStream().forEach((smartLampFacade) -> setBrightness(percent));
        return this;
    }

    @Override
    public Future<CompositeLampFacade> fadeBrightnessFromTo(byte from, byte to, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeBrightnessFromTo(from, to, fadeStyle));
        return futureMe;
    }

    @Override
    public Future<CompositeLampFacade> fadeBrightnessTo(byte percent, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeBrightnessTo(percent, fadeStyle));
        return futureMe;
    }

    @Override
    public CompositeLampFacade setTemperature(int kelvin) throws UnsupportedOperationException {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.setTemperature(kelvin));
        return this;
    }

    @Override
    public Future<CompositeLampFacade> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeTemperatureFromTo(from, to, fadeStyle));
        return futureMe;
    }

    @Override
    public Future<CompositeLampFacade> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeTemperatureTo(kelvin, fadeStyle));
        return futureMe;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeColorFromTo(int[] from, int[] to, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeColorFromTo(from, to, fadeStyle));
        return futureMe;
    }

    @Override
    public Future<? extends SmartLampFacade> fadeColorTo(int[] to, FadeStyle fadeStyle) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.fadeColorTo(to, fadeStyle));
        return futureMe;
    }

    @Override
    public CompositeLampFacade setScene(byte scene) throws UnsupportedOperationException {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.setScene(scene));
        return this;
    }

    @Override
    public CompositeLampFacade setColor(int red, int green, int blue) {
        composites.parallelStream().forEach(smartLampFacade -> smartLampFacade.setColor(red, green, blue));
        return this;
    }


}
