package ch.sebpiller.iot.lamp.sequencer;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import static ch.sebpiller.iot.lamp.ColorHelper.parseColor;

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
            SmartLampScript smartLampScript = fromInputStream(new FileInputStream(filename));
            smartLampScript.setName(filename);
            return smartLampScript;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file not found: " + e, e);
        }
    }

    public static SmartLampScript fromInputStream(InputStream inputStream) {
        Yaml yaml = new Yaml();

        try (InputStream is = inputStream) {
            return new SmartLampScript(yaml.loadAs(is, YamlScript.class));
        } catch (YAMLException e) {
            throw new IllegalArgumentException("the document given is invalid: " + e, e);
        } catch (IOException e) {
            throw new IllegalStateException("io error: " + e, e);
        }
    }

    public SmartLampSequence getBeforeSequence() {
        String before = yamlScript.getBefore();
        if (StringUtils.isBlank(before)) {
            return SmartLampSequence.NOOP;
        }

        SmartLampSequence record = parseStep(new SmartLampSequence.PlayAllAtOneTimeSequence(), before);
        return record;
    }

    public SmartLampSequence getAfterSequence() {
        String after = yamlScript.getAfter();
        if (StringUtils.isBlank(after)) {
            return SmartLampSequence.NOOP;
        }

        SmartLampSequence record = parseStep(new SmartLampSequence.PlayAllAtOneTimeSequence(), after);
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

    private SmartLampSequence parseStep(SmartLampSequence record, String s) {
        String step = s;

        if (StringUtils.isBlank(step)) {
            // empty line means a pause
            record = record.pause();
        } else {
            record = record.start();
            String token;

            StringTokenizer tokenizer = new StringTokenizer(step, ";");
            while (tokenizer.hasMoreElements() && (token = tokenizer.nextToken()) != null) {
                StringTokenizer instructionTokenizer = new StringTokenizer(token, "=");
                String key = instructionTokenizer.nextToken();
                String value = null;
                if (instructionTokenizer.hasMoreElements()) {
                    value = instructionTokenizer.nextToken();
                }

                switch (key.toLowerCase()) {
                    case "pause":
                        record = record.pause();
                        break;
                    case "on":
                        record = record.power(true);
                        break;
                    case "off":
                        record = record.power(false);
                        break;
                    case "power":
                        record = record.power("1".equals(value) || "on".equals(value) || "true".equals(value));
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
                    case "scene":
                        record = record.setScene(Byte.parseByte(value));
                        break;
                    case "color":
                        int[] color = parseColor(value);
                        record = record.setColor(color[0], color[1], color[2]);
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
            }
        }


        record = record.end();
        return record;
    }
}
