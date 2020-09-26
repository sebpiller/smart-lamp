package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.lamp.LoggingLamp;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.tictac.TicTac;
import ch.sebpiller.tictac.TicTacBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartLampSequencerTest {
    private static final Logger LOG = LoggerFactory.getLogger(SmartLampSequencerTest.class);

    @Test
    public void testBasic() throws InterruptedException {
        final SmartLampSequencer playback = SmartLampSequencer.record()
                // beat #1
                .start().flash(1).end();

        final SmartLampFacade lamp = new LoggingLamp();

        TicTac ticTac = new TicTacBuilder()
                .connectedToBpm(() -> 120)
                .withListener((ticOrTac, bpm) -> playback.playNext(lamp))
                .build();

        Thread.sleep(20_000);
        ticTac.stop();
    }

    @Test
    public void testSequencer() throws InterruptedException {
        final SmartLampSequencer boomBoomBoomBoom = SmartLampSequencer.record()
                .start().flash(1).end()
                .start().flash(1).end()
                .start().flash(1).end()
                .start().flash(1).end();

        final SmartLampSequencer playback = SmartLampSequencer.record()
                // beat #1
                .start().run(() -> LOG.info("***********************")).flash(3).end()
                // beat #2
                .start().run(() -> LOG.info(".")).flash(1).end()
                // beat #3
                .start().run(() -> LOG.info(":")).flash(1).end()
                // beat #4
                .start().run(() -> LOG.info(".")).flash(1).end()
                // beat #5
                .start().run(() -> LOG.info("+++++++++++++++++++++++")).end()
                // beat #6
                .start().run(() -> LOG.info(".")).flash(1).end()
                // beat #7
                .start().run(() -> LOG.info(":")).flash(1).end()
                // beat #8
                .start().run(() -> LOG.info(":")).flash(1).end()
                // beat 9-12
                .then(boomBoomBoomBoom)
                // beat 13-16
                .then(boomBoomBoomBoom)
                //
                ;

        LOG.warn("{}", ReflectionToStringBuilder.reflectionToString(playback));


        final SmartLampFacade lamp = new LoggingLamp();

        TicTac ticTac = new TicTacBuilder()
                .connectedToBpm(() -> 120)
                .withListener((ticOrTac, bpm) -> playback.playNext(lamp))
                .build();

        Thread.sleep(20_000);

        ticTac.stop();
    }
}