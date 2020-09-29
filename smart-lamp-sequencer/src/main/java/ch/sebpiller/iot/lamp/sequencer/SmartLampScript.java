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
 * A parser that produce {@link SmartLampSequence} reading instructions in a yaml formatted resource.
 */
public class SmartLampScript {
    private YamlScript yamlScript;
    private String name;

    public SmartLampScript() {
    }

    private SmartLampScript(YamlScript yamlScript) {
        this.yamlScript = Objects.requireNonNull(yamlScript);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static class YamlScript {
        private String before, after;
        private String[] loop;
        private Map<String, String[]> sequences = new HashMap<>();

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String[] getLoop() {
            return loop;
        }

        public void setLoop(String[] loop) {
            this.loop = loop;
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


    public static SmartLampScript embeddedScript(String scriptName) {
        InputStream is = SmartLampScript.class.getResourceAsStream("/embedded-scripts/" + scriptName + ".yaml");
        if (is == null) {
            throw new IllegalArgumentException(scriptName + " is not an existing script");
        }

        SmartLampScript smartLampScript = fromInputStream(is);
        smartLampScript.setName(scriptName);
        return smartLampScript;
    }

    public static SmartLampScript fromFile(String filename) {
        try {
            return fromInputStream(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("file not found: " + e, e);
        }
    }

    public static SmartLampScript fromInputStream(InputStream is) {
        Yaml yaml = new Yaml();

        try {
            return new SmartLampScript(yaml.loadAs(is, YamlScript.class));
        } catch (YAMLException ye) {
            throw new IllegalArgumentException("the document given is invalid: " + ye, ye);
        }
    }

    public SmartLampSequence getBeforeSequence() {
        String before = yamlScript.getBefore();
        if (StringUtils.isBlank(before)) {
            return new SmartLampSequence();
        }

        SmartLampSequence record = parseStep(new PlayAllAtOneTimeSequence(), before);
        return record;
    }

    public SmartLampSequence getAfterSequence() {
        String after = yamlScript.getAfter();
        if (StringUtils.isBlank(after)) {
            return new SmartLampSequence();
        }

        SmartLampSequence record = parseStep(new PlayAllAtOneTimeSequence(), after);
        return record;
    }

    public Map<String, SmartLampSequence> getSequences() {
        Map<String, SmartLampSequence> sequences = new HashMap<>();

        for (Map.Entry<String, String[]> current : yamlScript.getSequences().entrySet()) {
            SmartLampSequence record = SmartLampSequence.record();

            for (String step : current.getValue()) {
                SmartLampSequence currentSeq = parseStep(record, step);
                sequences.put(current.getKey(), currentSeq);
            }
        }

        return sequences;
    }

    public SmartLampSequence getSequence(String name) {
        return getSequences().get(name);
    }

    public SmartLampSequence buildMainLoopSequence() {
        String[] steps = yamlScript.getLoop();

        if (steps == null || steps.length <= 0) {
            return SmartLampSequence.NOOP;
        }

        SmartLampSequence record = SmartLampSequence.record();

        for (String step : steps) {
            record = parseStep(record, step);
        }

        return record;
    }

    private SmartLampSequence parseStep(SmartLampSequence record, String step) {
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
                    SmartLampSequence sequence = getSequence(value);
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
