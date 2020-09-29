package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sequence that plays all the given callbacks at the same frame ({@link #play(SmartLampFacade)}
 * will loop through all the content and play it sequentially).
 */
class PlayAllAtOneTimeSequence extends SmartLampSequence {
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
