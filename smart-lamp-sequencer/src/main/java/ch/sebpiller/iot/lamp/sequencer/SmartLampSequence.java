package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Fluent api exposing the interface of a {@link SmartLampFacade}, but that actually only records a sequence of call
 * to be played in a loop later.
 */
public class SmartLampSequence implements SmartLampFacade {
    public static final SmartLampSequence NOOP = new SmartLampSequence() {
        @Override
        public SmartLampSequence play(SmartLampFacade realLamp) {
            // NOOP
            return this;
        }

        @Override
        void add(InvokeOnSmartLamp c) {
            throw new UnsupportedOperationException("unable to populate a NOOP implementation");
        }
    };
    private static final Logger LOG = LoggerFactory.getLogger(SmartLampSequence.class);
    protected final List<InvokeOnSmartLamp> callables = Collections.synchronizedList(new ArrayList<>());
    private int playIndex = 0;
    private SmartLampSequence parent;

    public SmartLampSequence() {
        this(null);
    }

    protected SmartLampSequence(SmartLampSequence parent) {
        this.parent = parent;
    }

    public static SmartLampSequence record() {
        return new SmartLampSequence();
    }

    @Override
    public SmartLampSequence sleep(int millis) {
        add(facade -> facade.sleep(millis));
        return this;
    }

    public SmartLampSequence play(SmartLampFacade realLamp) {
        synchronized (callables) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("invoking callable #{} of {}", playIndex, callables.size());
            }
            InvokeOnSmartLamp invoke = null;

            if (!callables.isEmpty()) {
                invoke = callables.get(playIndex++);

                if (playIndex >= callables.size()) {
                    playIndex = 0;
                }
            }

            if (invoke != null) {
                invoke.invoke(realLamp);
            }
        }

        return this;
    }

    void add(InvokeOnSmartLamp c) {
        callables.add(c);
    }

    @Override
    public SmartLampSequence power(boolean on) {
        add(facade -> facade.power(on));
        return this;
    }

    @Override
    public SmartLampSequence setBrightness(byte percent) throws UnsupportedOperationException {
        add(facade -> facade.setBrightness(percent));
        return this;
    }

    @Override
    public Future<SmartLampSequence> fadeBrightnessFromTo(byte from, byte to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeBrightnessFromTo(from, to, fadeStyle));
        return present();
    }

    /**
     * A fake future object that has already been completed (we may call it the "present").
     */
    private FutureTask<SmartLampSequence> present() {
        return new FutureTask<SmartLampSequence>(() -> this) {
            @Override
            public SmartLampSequence get() {
                return SmartLampSequence.this;
            }
        };
    }

    @Override
    public Future<SmartLampSequence> fadeBrightnessTo(byte percent, FadeStyle fadeStyle) {
        add(facade -> facade.fadeBrightnessTo(percent, fadeStyle));
        return present();
    }

    @Override
    public SmartLampSequence setTemperature(int kelvin) {
        add(facade -> facade.setTemperature(kelvin));
        return this;
    }

    @Override
    public Future<SmartLampSequence> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeTemperatureFromTo(from, to, fadeStyle));
        return present();
    }

    @Override
    public Future<SmartLampSequence> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle) {
        add(facade -> facade.fadeTemperatureTo(kelvin, fadeStyle));
        return present();
    }

    @Override
    public SmartLampSequence setScene(byte scene) throws UnsupportedOperationException {
        add(facade -> facade.setScene(scene));
        return this;
    }

    /**
     * Invoke start to begin the recording of several actions to play sequentially during the same frame (beat).
     */
    public SmartLampSequence start() {
        // protect against multiple call to start()
        if (parent != null) {
            return this;
        }

        final SmartLampSequence inner = new PlayAllAtOneTimeSequence(this);
        add(facade -> inner.play(facade));
        return inner;
    }

    public SmartLampSequence end() {
        // protect against multiple call to end()
        return parent == null ? this : parent;
    }

    public SmartLampSequence pause() {
        return start().end();
    }

    public SmartLampSequence pause(int beats) {
        SmartLampSequence seq = this;

        for (int i = 0; i < beats; i++) {
            seq = seq.pause();
        }

        return seq;
    }

    public SmartLampSequence flash(int times) {
        SmartLampSequence start = start();

        for (int i = 0; i < times; i++) {
            start = start
                    .setBrightness((byte) 100).sleep(30)
                    .setBrightness((byte) 0).sleep(30)
            ;
        }

        return start;
    }

    /** Add an arbitrary execution of code to the current sequence. */
    public SmartLampSequence run(Runnable r) {
        add(new InvokeOnSmartLamp() {
            private int errorCount = 0;

            @Override
            public Object invoke(SmartLampFacade facade) {
                try {
                    r.run();
                } catch (Exception e) {
                    errorCount++;

                    if (errorCount < 10) {
                        LOG.error("error #" + errorCount + " in function call: " + e, e);
                    } else if (errorCount == 10) {
                        LOG.error("no more errors will be reported for this function.");
                    }
                }

                return SmartLampSequence.this;
            }
        });

        return this;
    }

    /**
     * Append another sequence to the one actually in creation.
     *
     * @param next Steps to append to the current content. Later modifications of this object will not be reflected.
     */
    public SmartLampSequence then(SmartLampSequence next) {
        callables.addAll(next.callables);
        return this;
    }

    @FunctionalInterface
    interface InvokeOnSmartLamp {
        Object invoke(SmartLampFacade facade);
    }

    /**
     * A sequence that plays all the given callbacks at the same frame ({@link #play(SmartLampFacade)}
     * will loop through all the content and play it sequentially).
     */
    static class PlayAllAtOneTimeSequence extends SmartLampSequence {
        private static final Logger LOG = LoggerFactory.getLogger(PlayAllAtOneTimeSequence.class);

        PlayAllAtOneTimeSequence() {
            super();
        }

        PlayAllAtOneTimeSequence(SmartLampSequence parent) {
            super(parent);
        }

        @Override
        public SmartLampSequence play(SmartLampFacade realLamp) {
            synchronized (callables) {
                LOG.debug("invoking {} callables in one step", callables.size());
                callables.forEach(e -> e.invoke(realLamp));
            }

            return this;
        }

    }
}
