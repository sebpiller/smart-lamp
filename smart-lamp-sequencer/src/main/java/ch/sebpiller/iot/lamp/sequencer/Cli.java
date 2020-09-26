package ch.sebpiller.iot.lamp.sequencer;

import ch.sebpiller.iot.bluetooth.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.bluetooth.luke.roberts.lamp.f.LukeRobertsLampF;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.sound.beatdetect.BpmSourceAudioListener;
import ch.sebpiller.tictac.BpmSource;
import ch.sebpiller.tictac.TicTac;
import ch.sebpiller.tictac.TicTacBuilder;
import org.apache.commons.cli.*;

public class Cli {
    // 4 beats
    private static final SmartLampSequencer boomBoomBoomBoom = SmartLampSequencer.record()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            //
            ;

    private static final SmartLampSequencer playback = SmartLampSequencer.record()
            .then(boomBoomBoomBoom)
            .then(boomBoomBoomBoom)
            .then(boomBoomBoomBoom)
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(1).end()
            .start().flash(3).end()
            //
            ;

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

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
                .type(Long.class)
                .build());

        LukeRoberts.LampF.Config config = LukeRoberts.LampF.Config.getDefaultConfig();
        long timeout = 0;
        int bpm = 0;

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

            if (line.hasOption("timeout")) {
                timeout = Long.parseLong(line.getOptionValue("timeout"));
            }
            if (line.hasOption("bpm")) {
                bpm = Integer.parseInt(line.getOptionValue("bpm"));
            }
            if (line.hasOption("script")) {
                // TODO implement scripted sequence
                throw new RuntimeException("not implemented yet");
            }
        } catch (ParseException exp) {
            System.err.println("Unexpected exception:" + exp.getMessage());
            System.exit(-1);
        }

        BpmSource source;
        if (bpm > 0) {
            int finalBpm = bpm;
            source = () -> finalBpm;
        } else {
            source = BpmSourceAudioListener.getBpmFromLineIn();
        }

        LukeRobertsLampF lukeRobertsLampF = new LukeRobertsLampF(config);
        lukeRobertsLampF.selectScene((byte) 4);

//        final SmartLampFacade lamp = CompositeLampFacade.from(
//                new LoggingLamp(), lukeRobertsLampF
//        );
        final SmartLampFacade lamp = lukeRobertsLampF;

        TicTac ticTac = new TicTacBuilder()
                .connectedToBpm(source)
                .withListener((ticOrTac, _bpm) -> playback.playNext(lamp))
                //.withListener((ticOrTac, bpm) -> LOG.info(ticOrTac ?"tic":"  tac"))
                .build();

        try {
            if (timeout <= 0) {
                ticTac.waitTermination();
            } else {
                try {
                    Thread.sleep(timeout * 1_000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } finally {
            ticTac.stop();
            System.exit(0);
        }
    }
}
