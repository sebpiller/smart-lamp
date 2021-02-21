package ch.sebpiller.iot.lamp.sequencer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmartLampYamlScriptTest {
    @Test
    public void testParseBoomFile() {
        SmartLampScript smartLampScript = SmartLampScript
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/boom.yaml"));
        smartLampScript.buildMainLoopSequence();
    }

    @Test
    public void testParseSwitchTemperatureFile() {
        SmartLampScript smartLampScript = SmartLampScript
                .fromInputStream(getClass().getResourceAsStream("/embedded-scripts/temperature.yaml"));
        smartLampScript.buildMainLoopSequence();
    }

    @Test
    public void testParseCommentedFile() {
        SmartLampScript smartLampScript = SmartLampScript
                .fromInputStream(getClass().getResourceAsStream("/scripts/commented.yaml"));
        SmartLampSequence smartLampSequence = smartLampScript.buildMainLoopSequence();

        smartLampSequence.callables.forEach(System.out::println);
    }

    @Test
    public void testParseBadlyFormattedFile() {
        assertThrows(IllegalArgumentException.class, () -> {
            SmartLampScript smartLampScript = SmartLampScript
                    .fromInputStream(getClass().getResourceAsStream("/scripts/badly_formatted.yaml"));
            smartLampScript.buildMainLoopSequence();
        });
    }

    @Test
    public void testParseSequenceNotDefined() {
        assertThrows(IllegalArgumentException.class, () -> {
            SmartLampScript smartLampScript = SmartLampScript
                    .fromInputStream(getClass().getResourceAsStream("/scripts/sequence_not_defined.yaml"));
            smartLampScript.buildMainLoopSequence();
        });
    }

}