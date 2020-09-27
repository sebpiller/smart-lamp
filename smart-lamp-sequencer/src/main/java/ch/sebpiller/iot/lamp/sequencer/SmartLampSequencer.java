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
public class SmartLampSequencer implements SmartLampFacade {
    private static final Logger LOG = LoggerFactory.getLogger(SmartLampSequencer.class);
    protected final List<InvokeOnSmartLamp> callables = Collections.synchronizedList(new ArrayList<>());
    private int playIndex = 0;
    private SmartLampSequencer parent;

    protected SmartLampSequencer() {
        this(null);
    }

    protected SmartLampSequencer(SmartLampSequencer parent) {
        this.parent = parent;
    }

    public static SmartLampSequencer record() {
        return new SmartLampSequencer();
    }

    public SmartLampSequencer sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // nothing
        }
        return this;
    }

    public SmartLampSequencer playNext(SmartLampFacade realFacade) {
        synchronized (callables) {
            if (LOG.isDebugEnabled())
                LOG.debug("invoking callable #{} of {}", playIndex, callables.size());
            InvokeOnSmartLamp invoke = null;

            if (!callables.isEmpty()) {
                invoke = callables.get(playIndex++);

                if (playIndex >= callables.size()) {
                    playIndex = 0;
                }
            }

            if (invoke != null) {
                invoke.invoke(realFacade);
            }
        }

        return this;
    }

    private void add(InvokeOnSmartLamp c) {
        callables.add(c);
    }

    @Override
    public SmartLampSequencer power(boolean on) {
        add(facade -> facade.power(on));
        return this;
    }

    @Override
    public SmartLampSequencer setBrightness(byte percent) throws UnsupportedOperationException {
        add(facade -> facade.setBrightness(percent));
        return this;
    }

    @Override
    public Future<SmartLampSequencer> fadeBrightnessFromTo(byte from, byte to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeBrightnessFromTo(from, to, fadeStyle));
        return present();
    }

    /**
     * A fake future object that has already been completed (we may call it the "present").
     */
    private FutureTask<SmartLampSequencer> present() {
        return new FutureTask<SmartLampSequencer>(() -> this) {
            @Override
            public SmartLampSequencer get() {
                return SmartLampSequencer.this;
            }
        };
    }

    @Override
    public Future<SmartLampSequencer> fadeBrightnessTo(byte percent, FadeStyle fadeStyle) {
        add(facade -> facade.fadeBrightnessTo(percent, fadeStyle));
        return present();
    }

    @Override
    public SmartLampSequencer setTemperature(int kelvin) {
        add(facade -> facade.setTemperature(kelvin));
        return this;
    }

    @Override
    public Future<SmartLampSequencer> fadeTemperatureFromTo(int from, int to, FadeStyle fadeStyle) {
        add(facade -> facade.fadeTemperatureFromTo(from, to, fadeStyle));
        return present();
    }

    @Override
    public Future<SmartLampSequencer> fadeTemperatureTo(int kelvin, FadeStyle fadeStyle) {
        add(facade -> facade.fadeTemperatureTo(kelvin, fadeStyle));
        return present();
    }

    /**
     * Invoke start to begin the recording of several actions to play sequentially during the same frame (beat).
     */
    public SmartLampSequencer start() {
        // protect against multiple call to start()
        if (parent != null) {
            return this;
        }

        final SmartLampSequencer inner = new PlayAllAtOneTimeSequencer();
        add(facade -> inner.playNext(facade));
        return inner;
    }

    public SmartLampSequencer end() {
        // protect against multiple call to end()
        return parent == null ? this : parent;
    }

    public SmartLampSequencer pause() {
        return start().end();
    }

    public SmartLampSequencer pause(int beats) {
        SmartLampSequencer seq = this;

        for (int i = 0; i < beats; i++) {
            seq = seq.pause();
        }

        return seq;
    }

    public SmartLampSequencer flash(int times) {
        SmartLampSequencer start = start();

        for (int i = 0; i < times; i++) {
            start = start
                    .setBrightness((byte) 100).sleep(30)
                    .setBrightness((byte) 0).sleep(30)
            ;
        }

        return start;
    }

    public SmartLampSequencer run(Runnable r) {
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

                return SmartLampSequencer.this;
            }
        });

        return this;
    }

    /**
     * Append another sequence to the one actually in creation.
     *
     * @param next Steps to append to the current content. Later modifications of this object will not be reflected.
     */
    public SmartLampSequencer then(SmartLampSequencer next) {
        callables.addAll(next.callables);
        return this;
    }

    @FunctionalInterface
    interface InvokeOnSmartLamp {
        Object invoke(SmartLampFacade facade);
    }
}
