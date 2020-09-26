package ch.sebpiller.iot.lamp.sequencer;

import org.junit.Test;

public class ScriptParserTest {
    @Test
    public void testParseBoomFile() {
        SmartLampSequencer sequence = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/scripts/boom.yaml"))//
                .buildSequence()//
                ;
    }

}