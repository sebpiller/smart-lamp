package ch.sebpiller.iot.lamp.sequencer.luke.roberts.lamp.f;

import ch.sebpiller.beatdetect.BpmSourceAudioListener;
import ch.sebpiller.iot.bluetooth.lamp.luke.roberts.LampFBle;
import ch.sebpiller.iot.bluetooth.lamp.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.iot.lamp.cli.SmartLampInteractive;
import ch.sebpiller.iot.lamp.sequencer.SmartLampScript;
import ch.sebpiller.iot.lamp.sequencer.SmartLampSequence;

import ch.sebpiller.metronome.Metronome;
import ch.sebpiller.metronome.MetronomeBuilder;
import ch.sebpiller.metronome.Tempo;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.PicocliException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "java -jar luke-roberts-lamp-f-cli.jar",
        footer = "NO Copyright - 2020",
        description = "Automated manipulation of a @|bold,underline Luke Roberts' Lamp F.|@",
        sortOptions = false,
        versionProvider = Cli.VersionProvider.class,
        header = {
                " _      _____             _                             ______ ",
                "| |    |  __ \\           | |                           |  ____|",
                "| |    | |__) |  ______  | |     __ _ _ __ ___  _ __   | |__   ",
                "| |    |  _  /  |______| | |    / _` | '_ ` _ \\| '_ \\  |  __|  ",
                "| |____| | \\ \\           | |___| (_| | | | | | | |_) | | |     ",
                "|______|_|  \\_\\          |______\\__,_|_| |_| |_| .__/  |_|     ",
                "                                               | |             ",
                "                                               |_|             "
        }
)
public class Cli implements Callable<Integer> {
    public static final String ARTIFACT_ID = "luke-roberts-lamp-f-cli";
    public static final String QUEUE = "lampf";
    private ConnectionFactory connectionFactory;

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            String implementationVersion = getClass().getPackage().getImplementationVersion();
            if (implementationVersion != null) {
                return new String[]{implementationVersion};
            }

            String name = "/" + ARTIFACT_ID + ".version";
            InputStream versionsInfo = getClass().getResourceAsStream(name);
            if (versionsInfo == null) {
                System.err.println("unable to find version info file.");
            } else {
                try (InputStream is = versionsInfo) {
                    Properties props = new Properties();
                    props.load(is);

                    assert props.getProperty("artifact").equals(ARTIFACT_ID);

                    return new String[]{props.getProperty("version") + " (built on " + props.getProperty("timestamp") + ")"};
                } catch (IOException e) {
                    System.err.println("unable to load file " + name);
                }
            }

            return new String[]{"unknown"};
        }
    }

    private String getVersion() {
        return new VersionProvider().getVersion()[0];
    }

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
            names = {"-v", "--version"},
            description = "Print version information to the console and exit.",
            arity = "0",
            versionHelp = true,
            type = Boolean.class
    )
    private Boolean cliParamVersion;

    @Option(
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
            type = Float.class
    )
    @Range(min = 20, max = 200)
    private Float cliParamTempo;

    @Option(
            order = 6,
            names = {"--amqp"},
            description = "[EXPERIMENTAL] Set the mode to AMQP (RabbitMq) queue listening mode. Will execute commands as messages are consumed from RabbitMq",
            paramLabel = "",
            type = Boolean.class
    )
    private Boolean cliParamAmqp = false;

    private Cli() {
    }

    public static void main(String[] args) {
        int exitCode = 0;

        CommandLine commandLine = new CommandLine(new Cli());

        try {
            commandLine.parseArgs(args);
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

    private void playSequenceOnLamp(SmartLampSequence script, SmartLampFacade lamp) {
        script.play(lamp);
    }

    private void playScriptOnLamp(SmartLampScript script, SmartLampFacade lamp) {
        try {
            playSequenceOnLamp(script.getBeforeSequence(), lamp);
            final SmartLampSequence loop = script.buildMainLoopSequence();

            // if we have a main loop, play it.
            if (!SmartLampSequence.NOOP.equals(loop)) {
                Tempo source;

                if (this.cliParamTempo == null || this.cliParamTempo <= 0) {
                    source = BpmSourceAudioListener.getBpmFromLineIn();
                } else {
                    float finalTempo = this.cliParamTempo;
                    source = () -> finalTempo;
                }

                try (Metronome ticTac = new MetronomeBuilder()
                        .withRhythm(source)
                        .withListener(new Metronome.MetronomeListener() {
                            private int i = 0;

                            @Override
                            public void missedBeats(int count, float bpm) {
                                this.i += count;
                                LOG.warn("missed beat {} (measure {})", this.i, (this.i / 4) + 1);
                                loop.skip(count);
                            }

                            @Override
                            public void beat(boolean ticOrTac, float bpm) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("beat {} (measure {})", this.i, (this.i / 4) + 1);
                                }

                                loop.play(lamp);
                                this.i++;
                            }
                        })
                        .build()) {
                    if (this.cliParamDuration > 0) {
                        try {
                            Thread.sleep(this.cliParamDuration * 1_000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    ticTac.waitTermination();
                }
            }
        } finally {
            playSequenceOnLamp(script.getAfterSequence(), lamp);
        }
    }

    private LampFBle buildLukeRobertsLampFFacadeFromSettings() {
        LukeRoberts.LampF.Config lampFConfig = LukeRoberts.LampF.Config.getDefaultConfig();
        lampFConfig = lampFConfig.merge(LukeRoberts.LampF.Config.loadFromStream(getClass().getResourceAsStream("/config/lampf.living.home.yaml")));

        // load config overrides from file if defined
        if (this.cliParamConfig != null) {
            LukeRoberts.LampF.Config c;
            try {
                c = LukeRoberts.LampF.Config.loadFromStream(new FileInputStream(this.cliParamConfig));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("file does not exist " + this.cliParamConfig, e);
            }

            lampFConfig = lampFConfig.merge(c);
        }

        // load cli flags overrides
        LukeRoberts.LampF.Config c = new LukeRoberts.LampF.Config();

        if (this.cliParamAdapter != null) {
            c.setLocalBtAdapter(this.cliParamAdapter);
        }
        if (this.cliParamMac != null) {
            c.setMac(this.cliParamMac);
        }
        lampFConfig = lampFConfig.merge(c);
        // -----

        return new LampFBle(lampFConfig);
    }

    @Override
    public Integer call() {
        String asciiLampF = StringUtils.join(Cli.class.getAnnotation(Command.class).header(), "\n");
        asciiLampF = asciiLampF + "\n" + "version: " + getVersion() + "\n";
        LOG.info("booting app...\n{}\n", asciiLampF);

        // Validation of the injected configuration
        validateThis();

        SmartLampScript script = null;
        if (this.cliParamScript != null) {
            if (this.cliParamScript.toLowerCase().startsWith(EMBEDDED_PREFIX)) {
                script = SmartLampScript.embeddedScript(this.cliParamScript.substring(EMBEDDED_PREFIX.length()));
            } else {
                script = SmartLampScript.fromFile(this.cliParamScript);
            }
        }

        try (LampFBle lamp = buildLukeRobertsLampFFacadeFromSettings()) {
            boolean interactive = this.cliParamScript == null && !this.cliParamAmqp;
            if (interactive) {
                new SmartLampInteractive(lamp).run(asciiLampF);
            } else {
                if (this.cliParamAmqp) {
                    listenToAmqpAndExecuteOnLamp(lamp);
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
            }

            return 0;
        } catch (Exception e) {
            LOG.error("error: {}", e, e);
            return 1;
        }
    }

    private void listenToAmqpAndExecuteOnLamp(LampFBle lamp) {
        LOG.info("starting RabbitMq queue listening mode...");
        ConnectionFactory factory = getConnectionFactory();

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE, false, false, false, null);
            channel.exchangeDeclare("command", "direct", false);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, "command", "push");
            channel.basicConsume(QUEUE, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                LOG.info("consuming message: '{}'", message);

                try {
                    SmartLampScript script = SmartLampScript.fromSingleCommand(message);
                    playScriptOnLamp(script, lamp);
                    LOG.debug("message consumed!");
                } catch (Exception e) {
                    LOG.error("error running command", e);
                }
            }, consumerTag -> {
            });

            waitForever();
        } catch (Exception e) {
            LOG.error("Error connecting AMQP", e);
        }
    }

    private ConnectionFactory getConnectionFactory() {
        if (this.connectionFactory == null)
            this.connectionFactory = new Yaml().loadAs(getClass().getResourceAsStream("/config/amqp.rabbitmq.home.yaml"), ConnectionFactory.class);
        return this.connectionFactory;
    }

    private void waitForever() {
        boolean done = false;
        while (!done) {
            // pause the main thread forever - only kill-9 will quit the loop
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                done = true;
                Thread.currentThread().interrupt();
            }
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
                "cliParamConfig='" + this.cliParamConfig + '\'' +
                ", cliParamAdapter='" + this.cliParamAdapter + '\'' +
                ", cliParamMac='" + this.cliParamMac + '\'' +
                ", cliParamScript='" + this.cliParamScript + '\'' +
                ", cliParamDuration=" + this.cliParamDuration +
                ", cliParamTempo=" + this.cliParamTempo +
                ", cliParamAmqp=" + this.cliParamAmqp +
                '}';
    }
}
