package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.iot.lamp.impl.LoggingLamp;
import ch.sebpiller.metronome.Metronome;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmarlLampSequenceTest {
    private static final Logger LOG = LoggerFactory.getLogger(SmarlLampSequenceTest.class);

    @Test
    public void testBasic() throws InterruptedException {
        final SmartLampSequence playback = SmartLampSequence.begin()
                // beat #1
                .start().flash(1).end();

        final SmartLampFacade lamp = new LoggingLamp();

        Metronome metronome = new Metronome(()->120, (ticOrTac, bpm) -> playback.play(lamp));

        Thread.sleep(20_000);
        metronome.stop();
    }

    @Test
    public void testSequencer() throws InterruptedException {
        final SmartLampSequence boomBoomBoomBoom = SmartLampSequence.begin()
                .start().flash(1).end()
                .start().flash(1).end()
                .start().flash(1).end()
                .start().flash(1).end();

        final SmartLampSequence playback = SmartLampSequence.begin()
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

        Metronome ticTac = new Metronome(() -> 120,(ticOrTac, bpm) -> playback.play(lamp));

        Thread.sleep(20_000);

        ticTac.stop();
    }


    @Test
    public void testSequencerFromScript() throws InterruptedException {
        final SmartLampScript seq = SmartLampScript.fromInputStream(getClass().getResourceAsStream("/embedded-scripts/boom.yaml"));

        final SmartLampFacade lamp = new LoggingLamp();
        seq.getBeforeSequence().play(lamp);

        SmartLampSequence smarlLampSequence = seq.buildMainLoopSequence();

        Metronome ticTac = new Metronome(() -> 120,(ticOrTac, bpm) -> smarlLampSequence.play(lamp));

        Thread.sleep(20_000);
        seq.getAfterSequence().play(lamp);

        ticTac.stop();
    }


}