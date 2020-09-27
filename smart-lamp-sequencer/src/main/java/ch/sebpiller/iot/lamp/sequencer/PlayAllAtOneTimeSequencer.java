package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sequence that plays all the given callbacks at the same frame ({@link #playNext(SmartLampFacade)}
 * will loop through all the content and play it sequentially).
 */
class PlayAllAtOneTimeSequencer extends SmartLampSequencer {
    private static final Logger LOG = LoggerFactory.getLogger(PlayAllAtOneTimeSequencer.class);

    PlayAllAtOneTimeSequencer() {
        super();
    }

    PlayAllAtOneTimeSequencer(SmartLampSequencer parent) {
        super(parent);
    }

    @Override
    public SmartLampSequencer playNext(SmartLampFacade realFacade) {
        synchronized (callables) {
            LOG.debug("invoking {} callables in one step", callables.size());
            callables.forEach(e -> e.invoke(realFacade));
        }

        return this;
    }

}
