package ch.sebpiller.iot.lamp.sequencer;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * A parser that produce {@link SmartLampSequencer} reading instructions in a yaml formatted resource.
 */
public class ScriptParser {
    private Script script;

    public ScriptParser(Script script) {
        this.script = Objects.requireNonNull(script);
    }

    public static class Script {
        private String init;
        private String[] steps;

        public String getInit() {
            return init;
        }

        public void setInit(String init) {
            this.init = init;
        }

        public String[] getSteps() {
            return steps;
        }

        public void setSteps(String[] steps) {
            this.steps = steps;
        }
    }

    public static ScriptParser fromFile(String filename) {
        try {
            return fromInputStream(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("file not found: " + e, e);
        }
    }

    public static ScriptParser fromInputStream(InputStream is) {
        Yaml yaml = new Yaml();
        Script script = yaml.loadAs(is, Script.class);
        return new ScriptParser(script);
    }

    public SmartLampSequencer getInitialisationSequence() {
        SmartLampSequencer record = parseStep(SmartLampSequencer.record(), script.getInit());
        return record;
    }

    public SmartLampSequencer buildSequence() {
        SmartLampSequencer record = SmartLampSequencer.record();

        for (String step : script.getSteps()) {
            record = parseStep(record, step);
        }

        return record;
    }

    private SmartLampSequencer parseStep(SmartLampSequencer record, String step) {
        StringTokenizer tokenizer = new StringTokenizer(step, ";");
        String token;

        record = record.start();

        while (tokenizer.hasMoreElements() && (token = tokenizer.nextToken()) != null) {
            StringTokenizer instructionTokenizer = new StringTokenizer(token, "=");
            String key = instructionTokenizer.nextToken();
            String value = null;
            if (instructionTokenizer.hasMoreElements()) {
                value = instructionTokenizer.nextToken();
            }

            switch (key.toLowerCase()) {
                case "brightness":
                    record = record.setBrightness(Byte.parseByte(value));
                    break;
                case "temperature":
                    record = record.setTemperature(Integer.parseInt(value));
                    break;
                case "sleep":
                    record = record.sleep(Integer.parseInt(value));
                    break;
                default:
                    throw new RuntimeException("script parse error: could not understand " + key);
            }

            System.out.println("set " + key + " to " + value);
        }

        record = record.end();
        return record;
    }
}
