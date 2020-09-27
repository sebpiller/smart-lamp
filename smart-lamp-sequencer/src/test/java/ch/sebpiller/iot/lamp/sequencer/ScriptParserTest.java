package ch.sebpiller.iot.lamp.sequencer;

import org.junit.Test;

public class ScriptParserTest {
    @Test
    public void testParseBoomFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/boom.yaml"));
        scriptParser.buildSequence();
    }

    @Test
    public void testParseSwitchTemperatureFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/switch_temperature.yaml"));
        scriptParser.buildSequence();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadlyFormattedFile() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/scripts/badly_formatted.yaml"));
        scriptParser.buildSequence();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseSequenceNotDefined() {
        ScriptParser scriptParser = ScriptParser
                .fromInputStream(getClass().getResourceAsStream("/scripts/sequence_not_defined.yaml"));
        scriptParser.buildSequence();
    }

}