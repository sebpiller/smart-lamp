package ch.sebpiller.iot.lamp.sequencer;

import org.junit.Test;

public class ScriptParserTest {
    @Test
    public void testParseBoomFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/boom.yaml"));
        SmartLampSequencer init = scriptParser.getInitialisationSequence();
        SmartLampSequencer sequence = scriptParser.buildSequence();
    }

    @Test
    public void testParseSwitchTemperatureFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/switch_temperature.yaml"));

        SmartLampSequencer init = scriptParser.getInitialisationSequence();
        SmartLampSequencer sequence = scriptParser.buildSequence();
    }

    @Test(expected = Exception.class)
    public void testParseBadlyFormattedFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/scripts/badly_formatted.yaml"));

        SmartLampSequencer init = scriptParser.getInitialisationSequence();
        SmartLampSequencer sequence = scriptParser.buildSequence();
    }
    @Test(expected = Exception.class)
    public void testParseSequenceNotDefined() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/scripts/sequence_not_defined.yaml"));

        SmartLampSequencer init = scriptParser.getInitialisationSequence();
        SmartLampSequencer sequence = scriptParser.buildSequence();
    }

}