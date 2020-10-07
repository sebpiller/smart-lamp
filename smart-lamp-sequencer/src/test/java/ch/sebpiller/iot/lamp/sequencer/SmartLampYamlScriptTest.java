package ch.sebpiller.iot.lamp.sequencer;

import org.junit.Test;

import java.util.function.Consumer;

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

        smartLampSequence.callables.forEach(new Consumer<SmartLampSequence.InvokeOnSmartLamp>() {
            @Override
            public void accept(SmartLampSequence.InvokeOnSmartLamp invokeOnSmartLamp) {
                System.out.println(invokeOnSmartLamp);
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadlyFormattedFile() {
        SmartLampScript smartLampScript = SmartLampScript
                .fromInputStream(getClass().getResourceAsStream("/scripts/badly_formatted.yaml"));
        smartLampScript.buildMainLoopSequence();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseSequenceNotDefined() {
        SmartLampScript smartLampScript = SmartLampScript
                .fromInputStream(getClass().getResourceAsStream("/scripts/sequence_not_defined.yaml"));
        smartLampScript.buildMainLoopSequence();
    }

}