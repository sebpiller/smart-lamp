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

    /**
     * Using a single command in the form: "power=true;color=red;brightness=80".
     */
    public static SmartLampScript fromSingleCommand(String commandList) {
        return new SmartLampScript() {
            @Override
            public SmartLampSequence getBeforeSequence() {
                return parseStep(SmartLampSequence.begin(), commandList, null);
            }
        };
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

        public void setSequences(Map<String, String[]> sequences) {
            this.sequences = sequences;
        }

        public Map<String, String[]> getSequences() {
            return sequences;
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
        String before = yamlScript == null ? null : yamlScript.getBefore();
        if (StringUtils.isBlank(before)) {
            return SmartLampSequence.NOOP;
        }

        return parseStep(new SmartLampSequence.PlayAllAtOneTimeSequence(), before, this);
    }

    public SmartLampSequence getAfterSequence() {
        String after = yamlScript == null ? null : yamlScript.getAfter();
        if (StringUtils.isBlank(after)) {
            return SmartLampSequence.NOOP;
        }

        return parseStep(new SmartLampSequence.PlayAllAtOneTimeSequence(), after, this);
    }

    public Map<String, SmartLampSequence> getSequences() {
        Map<String, SmartLampSequence> sequences = new HashMap<>();

        for (Map.Entry<String, String[]> current : yamlScript.getSequences().entrySet()) {
            SmartLampSequence r = SmartLampSequence.begin();

            for (String step : current.getValue()) {
                SmartLampSequence currentSeq = parseStep(r, step, this);
                sequences.put(current.getKey(), currentSeq);
            }
        }

        return sequences;
    }

    public SmartLampSequence getSequence(String name) {
        return getSequences().get(name);
    }

    public SmartLampSequence buildMainLoopSequence() {
        String[] steps = yamlScript == null ? null : yamlScript.getLoop();

        if (steps == null || steps.length == 0) {
            return SmartLampSequence.NOOP;
        }

        SmartLampSequence r = SmartLampSequence.begin();

        for (String step : steps) {
            r = parseStep(r, step, this);
        }

        return r;
    }

    private static SmartLampSequence parseStep(SmartLampSequence r, String s, SmartLampScript script) {
        if (StringUtils.isBlank(s)) {
            // empty line means a pause
            r = r.pause();
        } else {
            r = r.start();
            String token;

            StringTokenizer tokenizer = new StringTokenizer(s, ";");
            while (tokenizer.hasMoreElements() && (token = tokenizer.nextToken()) != null) {
                StringTokenizer instructionTokenizer = new StringTokenizer(token, "=");
                String key = instructionTokenizer.nextToken();
                String value = null;
                if (instructionTokenizer.hasMoreElements()) {
                    value = instructionTokenizer.nextToken();
                }

                if ("seq".equals(key)) {
                    if (script == null) {
                        throw new IllegalArgumentException("no script context, sequence lookup is impossible");
                    }
                    SmartLampSequence sequence = script.getSequence(value);
                    if (sequence == null) {
                        throw new IllegalArgumentException("the sequence '" + value + "' is not defined in the script context");
                    }
                    r = r.then(sequence);
                } else {
                    r = recordAction(r, key, value);
                }
            }
        }

        r = r.end();
        return r;
    }

    private static SmartLampSequence recordAction(SmartLampSequence r, String key, String value) {
        switch (key.toLowerCase()) {
            case "pause":
                r = r.pause();
                break;
            case "on":
                r = r.power(true);
                break;
            case "off":
                r = r.power(false);
                break;
            case "power":
                r = r.power("1".equals(value) || "on".equals(value) || "true".equals(value));
                break;
            case "brightness":
                if (value != null) {
                    r = r.setBrightness(Byte.parseByte(value));
                }
                break;
            case "temperature":
                if (value != null) {
                    r = r.setTemperature(Integer.parseInt(value));
                }
                break;
            case "sleep":
                if (value != null) {
                    r = r.sleep(Integer.parseInt(value));
                }
                break;
            case "scene":
                if (value != null) {
                    r = r.setScene(Byte.parseByte(value));
                }
                break;
            case "color":
                int[] color = parseColor(value);
                r = r.setColor(color[0], color[1], color[2]);
                break;
            default:
                throw new IllegalArgumentException("script parse error: could not understand " + key);
        }
        return r;
    }
}
