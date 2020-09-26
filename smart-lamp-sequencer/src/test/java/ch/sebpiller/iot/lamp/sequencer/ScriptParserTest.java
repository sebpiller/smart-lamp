package ch.sebpiller.iot.lamp.sequencer;

import org.junit.Test;

public class ScriptParserTest {
    @Test
    public void testParseBoomFile() {
        SmartLampSequencer sequence = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/boom.yaml"))//
                .buildSequence()//
                ;
    }
    @Test
    public void testParseSwitchTemperatureFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/switch_temperature.yaml"));

        SmartLampSequencer init = scriptParser.getInitialisationSequence();

        SmartLampSequencer sequence = scriptParser//
                .buildSequence()//
                ;
    }

}