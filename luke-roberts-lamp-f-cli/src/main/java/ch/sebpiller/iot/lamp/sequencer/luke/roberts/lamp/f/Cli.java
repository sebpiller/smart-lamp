package ch.sebpiller.iot.lamp.sequencer.luke.roberts.lamp.f;

import ch.sebpiller.iot.bluetooth.lamp.luke.roberts.LampFBle;
import ch.sebpiller.iot.bluetooth.lamp.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.iot.lamp.cli.SmartLampInteractive;
import ch.sebpiller.iot.lamp.sequencer.SmartLampScript;
import ch.sebpiller.iot.lamp.sequencer.SmartLampSequence;
import ch.sebpiller.sound.beatdetect.BpmSourceAudioListener;
import ch.sebpiller.tictac.BpmSource;
import ch.sebpiller.tictac.TicTac;
import ch.sebpiller.tictac.TicTacBuilder;
import org.hibernate.validator.constraints.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.PicocliException;

import javax.sound.sampled.*;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.lang.String.format;

@Command(
        name = "java -jar luke-roberts-lamp-f-cli.jar",
        footer = "NO Copyright - 2020",
        description = "Automated manipulation of a @|bold,underline Luke Roberts' Lamp F|@.",
        sortOptions = false
)
public class Cli implements Callable<Integer> {
    /**
     * Flashes the lamp 1 time at each beat, 4 times
     */
    private static final SmartLampSequence BOOM_BOOM_BOOM_BOOM = SmartLampSequence.record()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            //
            ;
    /**
     * The default sequence to be played when the user did not provide any script.
     */
    private static final SmartLampSequence DEFAULT_PLAYBACK = SmartLampSequence.record()
            .then(BOOM_BOOM_BOOM_BOOM)
            .then(BOOM_BOOM_BOOM_BOOM)
            .then(BOOM_BOOM_BOOM_BOOM)
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(3).end()
            //
            ;
    private static final String EMBEDDED_PREFIX = "embedded:";
    private static final Logger LOG = LoggerFactory.getLogger(Cli.class);

    @Option(
            order = -1,
            names = {"-h", "--help"},
            description = "Print usage to the console and exit.",
            arity = "0",
            usageHelp = true,
            type = Boolean.class
    )
    private Boolean cliParamHelp;

    @Option(
            order = 0,
            names = {"-c", "--config"},
            description = "A file containing bluetooth adapter to use and the lamp's MAC address.",
            paramLabel = "<CONFIG_FILE>",
            type = String.class
    )
    private String cliParamConfig;

    @Option(order = 1,
            names = {"-a", "--adapter"},
            description = "The bluetooth adapter to use.",
            defaultValue = "hci0",
            paramLabel = "<ADAPTER>",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
            type = String.class
    )
    private String cliParamAdapter;

    @Option(
            order = 2,
            names = {"-m", "--mac"},
            description = "The MAC address of the lamp to connect.",
            paramLabel = "<MAC>",
            //interactive = true,
            defaultValue = "C4:AC:05:42:73:A4",
            type = String.class
    )
    @Pattern(regexp = "^(([0-9A-F]{2}:){5}[0-9A-F]{2})?$")
    private String cliParamMac;

    @Option(
            order = 3,
            names = {"-s", "--script"},
            description = "Script to run. Can be prefixed with 'embedded:' to reference an internal script by its name. Eg. '--script " + EMBEDDED_PREFIX + "boom'.",
            paramLabel = "<SCRIPT>",
            type = String.class
    )
    @Pattern(regexp = "^(([\\w]+\\\\.yaml)|(embedded:(boom|bim|temperature|brightness|alarm|scene|dust)))?$")
    private String cliParamScript;

    @Option(
            order = 4,
            names = {"-d", "--duration", "--timeout"},
            description = "In scripted mode, set the duration to run the main loop before aborting (in seconds). 0=infinite",
            paramLabel = "<SECONDS>",
            type = long.class
    )
    @PositiveOrZero
    private Long cliParamDuration;

    @Option(
            order = 5,
            names = {"-t", "--tempo", "--rhythm"},
            description = "In scripted mode, set the frequency (in BPM) at which trigger tick events.",
            paramLabel = "<BPM>",
            type = Integer.class
    )
    @Range(min = 20, max = 200)
    private Integer cliParamTempo;

    private Cli() {
    }

    public static void main(String[] args) {
        int exitCode = 0;

        CommandLine commandLine = new CommandLine(new Cli());

        try {
            ParseResult pr = commandLine.parseArgs(args);
        } catch (PicocliException pce) {
            commandLine.usage(System.out);
            exitCode = -1;
            LOG.error("error: ", pce);
        }

        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
        } else {
            exitCode = commandLine.execute(args);
        }

        LOG.info("exiting with exit code {}", exitCode);
        System.exit(exitCode);
    }

    private String toAsciiArt(String text) {
        String url = format("http://artii.herokuapp.com/make?text=%s&font=%s",
                text.replaceAll("\\s", "+"), "standard");

        String asciiArt;

        try (Scanner s = new Scanner(new URL(url).openStream())) {
            asciiArt = s.useDelimiter("\\A").next();
        } catch (Exception e) {
            asciiArt = text;
        }

        return asciiArt;
    }

    private void playScriptOnLamp(SmartLampScript script, SmartLampFacade lamp) {
        try {
            script.getBeforeSequence().play(lamp);
            final SmartLampSequence loop = script.buildMainLoopSequence();

            // if we have a main loop, play it.
            if (!SmartLampSequence.NOOP.equals(loop)) {
                BpmSource source;

                if (cliParamTempo == null || cliParamTempo <= 0) {
                    source = BpmSourceAudioListener.getBpmFromLineIn();
                } else {
                    int finalTempo = cliParamTempo;
                    source = () -> finalTempo;
                }

                final AudioFilePlayer player = new AudioFilePlayer("/home/spiller/Bureau/Queen - Bohemian Rhapsody- The Original Soundtrack - 12 - Another One Bites the Dust.wav");

                try (TicTac ticTac = new TicTacBuilder()
                        .connectedToBpm(source)
                        .withListener(new TicTac.TicTacListener() {
                            int i = 0;

                            @Override
                            public void beat(boolean ticOrTac, float bpm) {
                                // Start queen playing
                                if (i == 0) {
                                    player.start();
                                }

                                LOG.warn("beat {} (measure {})", i, (i / 4) + 1);
                                loop.play(lamp);
                                i++;
                            }
                        })
                        .build()) {
                    if (cliParamDuration > 0) {
                        try {
                            Thread.sleep(cliParamDuration * 1_000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    } else {
                        ticTac.waitTermination();
                    }
                }
            }
        } finally {
            script.getAfterSequence().play(lamp);
        }
    }

    private LampFBle buildLukeRobertsLampFFacadeFromSettings() {
        LukeRoberts.LampF.Config lampFConfig = LukeRoberts.LampF.Config.getDefaultConfig();

        // load config overrides from file if defined
        if (cliParamConfig != null) {
            LukeRoberts.LampF.Config c;
            try {
                c = LukeRoberts.LampF.Config.loadFromStream(new FileInputStream(cliParamConfig));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("file does not exist " + cliParamConfig, e);
            }

            lampFConfig = lampFConfig.merge(c);
        }

        // load cli flags overrides
        LukeRoberts.LampF.Config c = new LukeRoberts.LampF.Config();

        if (cliParamAdapter != null) {
            c.setLocalBtAdapter(cliParamAdapter);
        }
        if (cliParamMac != null) {
            c.setMac(cliParamMac);
        }
        lampFConfig = lampFConfig.merge(c);
        // -----

        return new LampFBle(lampFConfig);
    }

    @Override
    public Integer call() {
        // Hello world !
        String asciiLampF = toAsciiArt("Lamp F");
        LOG.info("booting app...\n{}\n", asciiLampF);

        // Validation of the injected configuration
        validateThis();

        SmartLampScript script = null;
        if (cliParamScript != null) {
            if (cliParamScript.toLowerCase().startsWith(EMBEDDED_PREFIX)) {
                script = SmartLampScript.embeddedScript(cliParamScript.substring(EMBEDDED_PREFIX.length()));
            } else {
                script = SmartLampScript.fromFile(cliParamScript);
            }
        }

        LampFBle lamp = buildLukeRobertsLampFFacadeFromSettings();

        try {
            boolean interactive = cliParamScript == null;
            if (interactive) {
                new SmartLampInteractive(lamp).run(asciiLampF);
            } else {
                SmartLampScript scriptToPlay = Optional.ofNullable(script).orElse(new SmartLampScript() {
                    @Override
                    public SmartLampSequence getBeforeSequence() {
                        return SmartLampSequence.NOOP;
                    }

                    @Override
                    public SmartLampSequence getAfterSequence() {
                        return SmartLampSequence.NOOP;
                    }

                    @Override
                    public SmartLampSequence buildMainLoopSequence() {
                        return DEFAULT_PLAYBACK;
                    }
                });

                playScriptOnLamp(scriptToPlay, lamp);
            }

            return 0;
        } catch (Exception e) {
            LOG.error("error: " + e, e);
            return 1;
        } finally {
            lamp.close();
        }
    }

    private void validateThis() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Cli>> violations = validator.validate(this);

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            violations.forEach(cliConstraintViolation -> sb
                    .append(", '")
                    .append(cliConstraintViolation.getInvalidValue())
                    .append("' ")
                    .append(cliConstraintViolation.getMessage())
            );

            String s = sb.substring(2);

            LOG.error("invoked command was invalid: {}", s);
            throw new IllegalArgumentException("invalid configuration: " + s);
        }
    }

    @Override
    public String toString() {
        return "Cli{" +
                "cliParamConfig='" + cliParamConfig + '\'' +
                ", cliParamAdapter='" + cliParamAdapter + '\'' +
                ", cliParamMac='" + cliParamMac + '\'' +
                ", cliParamScript='" + cliParamScript + '\'' +
                ", cliParamDuration=" + cliParamDuration +
                ", cliParamTempo=" + cliParamTempo +
                '}';
    }


    static class AudioFilePlayer extends Thread {
        private final String file;

        AudioFilePlayer(String file) {
            this.file = file;
        }

        @Override
        public void run() {
            play(file);
        }

        public void play(String filePath) {
            final File file = new File(filePath);

            try {
                final AudioInputStream in = AudioSystem.getAudioInputStream(file);

                final AudioFormat outFormat = getOutFormat(in.getFormat());
                final DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);

                final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                if (line != null) {
                    line.open(outFormat);
                    line.start();
                    stream(AudioSystem.getAudioInputStream(outFormat, in), line);
                    line.drain();
                    line.stop();
                }

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private AudioFormat getOutFormat(AudioFormat inFormat) {
            final int ch = inFormat.getChannels();
            final float rate = inFormat.getSampleRate();
            return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
        }

        private void stream(AudioInputStream in, SourceDataLine line)
                throws IOException {
            final byte[] buffer = new byte[65536];
            for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
                line.write(buffer, 0, n);
            }
        }
    }
}
