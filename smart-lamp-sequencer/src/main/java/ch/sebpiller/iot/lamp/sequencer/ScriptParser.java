package ch.sebpiller.iot.lamp.sequencer;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * A parser that produce {@link SmartLampSequencer} reading an input stream containing the steps in yaml.
 */
public class ScriptParser {
    private Script script;

    public ScriptParser(Script script) {
        this.script = Objects.requireNonNull(script);
    }

    public static class Script {
        private String[] steps;

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

    public SmartLampSequencer buildSequence() {
        SmartLampSequencer record = SmartLampSequencer.record();


        for (String step : script.getSteps()) {
            StringTokenizer tokenizer = new StringTokenizer(step, ";");
            String token;

            record = record.start();

            while(tokenizer.hasMoreElements() && (token = tokenizer.nextToken()) != null) {
                System.out.println(token);


            }

            record = record.end();
        }

        return record;
    }
}
