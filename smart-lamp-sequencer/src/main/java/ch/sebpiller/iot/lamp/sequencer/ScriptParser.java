package ch.sebpiller.iot.lamp.sequencer;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
        private String before, after;
        private String[] steps;
        private Map<String, String[]> sequences = new HashMap<>();

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String[] getSteps() {
            return steps;
        }

        public void setSteps(String[] steps) {
            this.steps = steps;
        }

        public Map<String, String[]> getSequences() {
            return sequences;
        }

        public void setSequences(Map<String, String[]> sequences) {
            this.sequences = sequences;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }
    }


    public static ScriptParser embeddedScript(String scriptName) {
        InputStream is = ScriptParser.class.getResourceAsStream("/embedded-scripts/" + scriptName + ".yaml");
        if (is == null) {
            throw new IllegalArgumentException(scriptName + " is not an existing script");
        }

        return fromInputStream(is);
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

        try {
            return new ScriptParser(yaml.loadAs(is, Script.class));
        } catch (YAMLException ye) {
            throw new IllegalArgumentException("the document given is invalid: "+ye, ye);
        }
    }

    public SmartLampSequencer getBeforeSequence() {
        SmartLampSequencer record = parseStep(new PlayAllAtOneTimeSequencer(SmartLampSequencer.record()), script.getBefore());
        return record;
    }

    public SmartLampSequencer getAfterSequence() {
        SmartLampSequencer record = parseStep(new PlayAllAtOneTimeSequencer(SmartLampSequencer.record()), script.getAfter());
        return record;
    }

    public Map<String, SmartLampSequencer> getSequences() {
        Map<String, SmartLampSequencer> sequences = new HashMap<>();

        for (Map.Entry<String, String[]> current : script.getSequences().entrySet()) {
            SmartLampSequencer record = SmartLampSequencer.record();

            for (String step : current.getValue()) {
                SmartLampSequencer currentSeq = parseStep(record, step);
                sequences.put(current.getKey(), currentSeq);
            }
        }

        return sequences;
    }

    public SmartLampSequencer getSequence(String name) {
        return getSequences().get(name);
    }

    public SmartLampSequencer buildSequence() {
        SmartLampSequencer record = SmartLampSequencer.record();

        for (String step : script.getSteps()) {
            record = parseStep(record, step);
        }

        return record;
    }

    private SmartLampSequencer parseStep(SmartLampSequencer record, String step) {
        if (StringUtils.isBlank(step)) {
            return record;
        }

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
                case "on":
                    record = record.power(true);
                    break;
                case "off":
                    record = record.power(false);
                    break;
                case "power":
                    record = record.power("1".equals(value) || "on".equals(value));
                    break;
                case "brightness":
                    record = record.setBrightness(Byte.parseByte(value));
                    break;
                case "temperature":
                    record = record.setTemperature(Integer.parseInt(value));
                    break;
                case "sleep":
                    record = record.sleep(Integer.parseInt(value));
                    break;
                case "seq":
                    SmartLampSequencer sequence = getSequence(value);
                    if (sequence == null) {
                        throw new IllegalArgumentException("the sequence '" + value + "' is not defined in the sequences section");
                    }
                    record = record.then(sequence);
                    break;
                default:
                    throw new IllegalArgumentException("script parse error: could not understand " + key);
            }

            System.out.println("set " + key + " to " + value);
        }

        record = record.end();
        return record;
    }
}
