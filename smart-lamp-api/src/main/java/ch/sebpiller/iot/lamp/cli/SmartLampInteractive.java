package ch.sebpiller.iot.lamp.cli;

import ch.sebpiller.iot.lamp.SmartLampFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

import static ch.sebpiller.iot.lamp.ColorHelper.parseColor;


/**
 * A small text interface to command a lamp.
 */
public class SmartLampInteractive {
    private static final Logger LOG = LoggerFactory.getLogger(SmartLampInteractive.class);
    private final SmartLampFacade facade;
    private Scanner scanner;

    private int blinkOnTime = 100, blinkOffTime = 250, blinkCount = 20;
    private byte fadeBrightFrom = 0, fadeBrightTo = 100;
    private int fadeTempFrom = 2700, fadeTempTo = 4000;

    private SmartLampFacade.FadeStyle //
            fadeBrightStyle = SmartLampFacade.FadeStyle.NORMAL,//
            fadeTempStyle = SmartLampFacade.FadeStyle.NORMAL //
                    ;

    public SmartLampInteractive(SmartLampFacade facade) {
        this.facade = Objects.requireNonNull(facade);
    }

    public void run(String... args) {
        scanner = new Scanner(System.in);
        try {
            while (showMenu(args)) ;
        } finally {
            scanner.close();

            if (facade instanceof Closeable) {
                try {
                    ((Closeable) facade).close();
                } catch (IOException e) {
                    LOG.debug("error during lamp closing: " + e, e);
                }
            }
        }
    }

    private void cls() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    // return true when cli is done
    private boolean showMenu(String... args) {
        cls();

        // title
        if (args != null && args.length > 0) {
            System.out.println(args[0]);
        }

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("Smart Lamp - interactive mode");
        System.out.println("=============================");
        System.out.println();
        System.out.println("1. Change power status");
        System.out.println("2. Change scene");
        System.out.println("3. Change color");
        System.out.println("4. Change brightness value");
        System.out.println("5. Change temperature (main bulb only)");
        System.out.println("6. Fade brightness");
        System.out.println("7. Fade temperature (main bulb only)");
        System.out.println("8. Ping");
        System.out.println();
        System.out.println();
        System.out.println("q. Quit");
        System.out.println();
        System.out.println();
        System.out.print("Choose an action: ");

        String line = scanner.nextLine();
        switch (line) {
            case "1":
                System.out.print("On/off: ");
                line = scanner.nextLine();
                switch (line) {
                    case "0":
                    case "off":
                    case "false":
                        facade.power(false);
                        break;
                    case "1":
                    case "on":
                    case "true":
                        facade.power(true);
                        break;
                }

                break;
            case "2":
                System.out.print("Scene ID (0=power off): ");
                line = scanner.nextLine();
                facade.setScene(Byte.parseByte(line));
                break;
            case "3":
                System.out.print("Color ('white' or 'ffffff'): ");
                line = scanner.nextLine();
                int[] ints = parseColor(line);
                facade.setColor(ints[0], ints[1], ints[2]);
                break;
            case "4":
                System.out.print("Brightness percentage: ");
                line = scanner.nextLine();
                facade.setBrightness(Byte.parseByte(line));
                break;
            case "5":
                System.out.print("Temperature (2'700K - 4'000K): ");
                line = scanner.nextLine();
                facade.setTemperature(Integer.parseInt(line));
                break;
            case "6":
                System.out.println("Fade brightness : ");
                System.out.println("  from 1. **" + fadeBrightFrom + "%** to 2. **" + fadeBrightTo + "%** in 3. **" + fadeBrightStyle + "**");
                System.out.print("Run with g : ");

                line = scanner.nextLine();
                switch (line) {
                    case "1":
                        System.out.print("choose fade from : ");
                        line = scanner.nextLine();
                        fadeBrightFrom = Byte.parseByte(line);
                        break;
                    case "2":
                        System.out.print("choose fade to : ");
                        line = scanner.nextLine();
                        fadeBrightTo = Byte.parseByte(line);
                        break;
                    case "3":
                        System.out.print("choose fade style (" + Arrays.toString(SmartLampFacade.FadeStyle.values()) + ") : ");
                        line = scanner.nextLine();
                        fadeBrightStyle = SmartLampFacade.FadeStyle.valueOf(line.toUpperCase());
                        break;
                    case "g":
                    case "go":
                        facade.fadeBrightnessFromTo(fadeBrightFrom, fadeBrightTo, fadeBrightStyle);
                        break;
                }

                break;
            case "7":
                System.out.println("Fade temperature : ");
                System.out.println("  >> from 1. **" + fadeTempFrom + "K** to 2. **" + fadeTempTo + "K** in 3. **" + fadeTempStyle + "**: ");
                System.out.print("Run with g : ");

                line = scanner.nextLine();
                switch (line) {
                    case "1":
                        System.out.print("choose fade from : ");
                        line = scanner.nextLine();
                        fadeTempFrom = Optional.ofNullable(Integer.parseInt(line)).orElse(fadeTempFrom);
                        break;
                    case "2":
                        System.out.print("choose fade to : ");
                        line = scanner.nextLine();
                        fadeTempTo = Optional.ofNullable(Integer.parseInt(line)).orElse(fadeTempTo);
                        break;
                    case "3":
                        System.out.print("choose fade style (" + Arrays.toString(SmartLampFacade.FadeStyle.values()) + ") : ");
                        line = scanner.nextLine();
                        fadeTempStyle = Optional.ofNullable(SmartLampFacade.FadeStyle.valueOf(line.toUpperCase())).orElse(fadeTempStyle);
                        break;
                    case "g":
                    case "go":
                        facade.fadeTemperatureFromTo(fadeTempFrom, fadeTempTo, fadeTempStyle);
                        break;
                }

                break;
            case "8":
                System.err.println("not implemented yet");
                scanner.nextLine();
//                System.out.print("Ping version (1/2) : ");
//                line = scanner.nextLine();
//                switch (line) {
//                    case "1":
//                        facade.pingV1();
//                        System.out.println("Ping v1 sent");
//                        scanner.nextLine();
//                        break;
//                    case "2":
//                        facade.pingV2();
//                        System.out.println("Ping v2 sent");
//                        scanner.nextLine();
//                        break;
//                }

                break;
            case "q":
                return false;
            default:
                System.out.println();
                System.out.println("invalid option: " + line);
                scanner.nextLine();
        }

        return true;
    }
}
