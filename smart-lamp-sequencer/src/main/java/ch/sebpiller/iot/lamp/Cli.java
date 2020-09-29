package ch.sebpiller.iot.lamp;

import ch.sebpiller.iot.bluetooth.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.bluetooth.luke.roberts.lamp.f.LukeRobertsLampF;
import ch.sebpiller.iot.lamp.sequencer.SmartLampScript;
import ch.sebpiller.iot.lamp.sequencer.SmartLampSequence;
import ch.sebpiller.sound.beatdetect.BpmSourceAudioListener;
import ch.sebpiller.tictac.BpmSource;
import ch.sebpiller.tictac.TicTac;
import ch.sebpiller.tictac.TicTacBuilder;
import org.apache.commons.cli.*;

import java.util.Optional;

public class Cli {
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
    private static LukeRobertsLampF lukeRobertsLampF;

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();

        LukeRoberts.LampF.Config config = LukeRoberts.LampF.Config.getDefaultConfig();
        long timeout = 0;
        int bpmInt = 0;
        SmartLampScript givenScript = null;
        boolean cli = false;

        try {
            CommandLine line = parser.parse(options, args);

            LukeRoberts.LampF.Config c = new LukeRoberts.LampF.Config();
            if (line.hasOption("adapter")) {
                c.setLocalBtAdapter(line.getOptionValue("adapter"));
            }
            if (line.hasOption("mac")) {
                c.setMac(line.getOptionValue("mac"));
            }
            config = config.merge(c);

            if (line.hasOption("cli")) {
                cli = true;
            } else {
                if (line.hasOption("timeout")) {
                    timeout = Long.parseLong(line.getOptionValue("timeout"));
                }
                if (line.hasOption("bpm")) {
                    bpmInt = Integer.parseInt(line.getOptionValue("bpm"));
                }
                if (line.hasOption("script")) {
                    String script = line.getOptionValue("script");

                    if (script.toLowerCase().startsWith(EMBEDDED_PREFIX)) {
                        givenScript = SmartLampScript.embeddedScript(script.substring(EMBEDDED_PREFIX.length()));
                    } else {
                        givenScript = SmartLampScript.fromFile(script);
                    }
                }
            }
        } catch (ParseException exp) {
            System.err.println("Unexpected exception: " + exp.getMessage());
            exp.printStackTrace();
            System.exit(-1);
        }

        LukeRobertsLampF lukeRobertsLampF = getLKLampF(config);

        try {
            final SmartLampFacade lamp = lukeRobertsLampF;
            //final SmartLampFacade lamp = new LoggingLamp();
            if (cli) {
                new SmartLampCli(lamp).run();
            } else {
                SmartLampScript defaultScript = new SmartLampScript() {
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
                };

                SmartLampScript script = Optional.ofNullable(givenScript).orElse(defaultScript);

                try {
                    script.getBeforeSequence().play(lamp);
                    final SmartLampSequence loop = script.buildMainLoopSequence();

                    if (loop != SmartLampSequence.NOOP) {
                        BpmSource source;

                        if (bpmInt > 0) {
                            int finalBpm = bpmInt;
                            source = () -> finalBpm;
                        } else {
                            source = BpmSourceAudioListener.getBpmFromLineIn();
                        }

                        TicTac ticTac = new TicTacBuilder()
                                .connectedToBpm(source)
                                .withListener((ticOrTac, _bpm) -> loop.play(lamp))
                                .build();

                        if (timeout <= 0) {
                            ticTac.waitTermination();
                        } else {
                            try {
                                Thread.sleep(timeout * 1_000);
                            } catch (InterruptedException e) {
                                // ignore
                            }

                            ticTac.stop();
                        }
                    }
                } finally {
                    script.getAfterSequence().play(lamp);
                }
            }
        } finally {
            System.exit(0);
        }
    }

    private static LukeRobertsLampF getLKLampF(LukeRoberts.LampF.Config config) {
        // TODO make the implementation in use parameterizable
        if (lukeRobertsLampF == null) {
            lukeRobertsLampF = new LukeRobertsLampF(config);
            lukeRobertsLampF.selectScene((byte) 2);
        }

        return lukeRobertsLampF;
    }

    /**
     * Returns the list of options available from the command line.
     */
    private static Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder().longOpt("adapter")
                .desc("Bluetooth adapter to use")
                .hasArg()
                .argName("ADAPTER")
                .type(String.class)
                .build());
        options.addOption(Option.builder().longOpt("mac")
                .desc("MAC address of the device")
                .hasArg()
                .argName("MAC")
                .build());
        options.addOption(Option.builder().longOpt("cli")
                .desc("CLI mode")
                .build());
        options.addOption(Option.builder().longOpt("timeout")
                .desc("How much time in seconds to run the sequence (default=0, unlimited)")
                .hasArg()
                .argName("TIMEOUT")
                .type(Long.class)
                .build());
        options.addOption(Option.builder().longOpt("bpm")
                .desc("Run the sequencer at the given rate. If not specified, connects to system line-in and try to detect the beat from the ambient music.")
                .hasArg()
                .argName("BPM")
                .type(Long.class)
                .build());
        options.addOption(Option.builder().longOpt("script")
                .desc("Load a scripted sequence from an external file")
                .hasArg()
                .argName("SCRIPT")
                .type(String.class)
                .build());

        return options;
    }
}
