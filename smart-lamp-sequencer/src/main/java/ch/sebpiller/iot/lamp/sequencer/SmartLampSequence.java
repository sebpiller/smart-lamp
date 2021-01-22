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
    private final SmartLampSequence parent;

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

    /**
     * Skip #count steps (beats) of the sequence currently playing.
     * <p>
     * Useful if the thread currently running this sequence has not been able to beat at the appropriate instant.
     */
    public void skip(int count) {
        synchronized (callables) {
            for (int i = 0; i < count; i++) {
                nextCallable();
            }
        }
    }

    /**
     * Plays the next step defined in the current sequence. If no such step is defined, the loop starts again
     * from the beginning.
     *
     * @param realLamp The lamp to pilot.
     * @return This sequencer for chaining.
     */
    public SmartLampSequence play(SmartLampFacade realLamp) {
        synchronized (callables) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("invoking callable #{} of {}", playIndex, callables.size());
            }
            InvokeOnSmartLamp invoke = nextCallable();

            if (invoke != null) {
                invoke.invoke(realLamp);
            }
        }

        return this;
    }

    /**
     * Get the next step defined in this sequence as an instance of {@link InvokeOnSmartLamp}. Restart
     * at the beginning if the end is reached. Returns {@code null} if and only if this sequence is currently
     * empty (0 step defined).
     */
    private InvokeOnSmartLamp nextCallable() {
        InvokeOnSmartLamp invoke = null;

        synchronized (callables) {
            if (!callables.isEmpty()) {
                invoke = callables.get(playIndex++);

                if (playIndex >= callables.size()) {
                    playIndex = 0;
                }
            }
        }

        return invoke;
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
    public SmartLampSequence setBrightness(byte percent) {
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
    public Future<? extends SmartLampFacade> fadeColorFromTo(int[] from, int[] to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeColorFromTo(from, to, fadeStyle));
        return present();
    }

    @Override
    public Future<? extends SmartLampFacade> fadeColorTo(int[] to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeColorTo(to, fadeStyle));
        return present();
    }

    @Override
    public SmartLampSequence setScene(byte scene) {
        add(facade -> facade.setScene(scene));
        return this;
    }

    @Override
    public SmartLampSequence setColor(int red, int green, int blue) {
        add(facade -> facade.setColor(red, green, blue));
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
        add(inner::play);
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

    /**
     * Adds an arbitrary execution of code to the current sequence.
     */
    public SmartLampSequence run(Runnable r) {
        add(new InvokeOnSmartLamp() {
            private int errorCount = 0;

            @Override
            public void invoke(SmartLampFacade facade) {
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
        void invoke(SmartLampFacade facade);
    }

    /**
     * A sequence that plays all the given callbacks at the same frame ({@link #play(SmartLampFacade)}.
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
