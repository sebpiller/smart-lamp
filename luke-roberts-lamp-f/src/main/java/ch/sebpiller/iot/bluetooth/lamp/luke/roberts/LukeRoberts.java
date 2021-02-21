package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implements Bluetooth format from Luke Roberts'.
 */
public final class LukeRoberts {
    public static class LampF {
        private static final byte COMMAND_PREFIX = (byte) 0xA0;
        private static final byte VERSION_1 = 0x01;
        private static final byte VERSION_2 = 0x02;

        /**
         * @deprecated as per Luke Roberts' documentation, this characteristics is now deprecated. It was used to change
         * the scene displayed.
         */
        @Deprecated
        static String UUID_CHARACTERISTICS_SELECT_SCENE_SERVICE = "44092844-0567-11e6-b862-0002a5d5c51b";

        public enum Command {
            PING_V1(COMMAND_PREFIX, VERSION_1, (byte) 0x00, bytes -> bytes.length == 0),
            PING_V2(COMMAND_PREFIX, VERSION_2, (byte) 0x00, bytes -> bytes.length == 0),
            QUERY_SCENE(COMMAND_PREFIX, VERSION_1, (byte) 0x01),
            IMMEDIATE_LIGHT(COMMAND_PREFIX, VERSION_1, (byte) 0x02),
            BRIGHTNESS(COMMAND_PREFIX, VERSION_1, (byte) 0x03, bytes -> bytes.length == 1),
            COLOR_TEMP(COMMAND_PREFIX, VERSION_1, (byte) 0x04),
            SELECT_SCENE(COMMAND_PREFIX, VERSION_2, (byte) 0x05),
            NEXT_SCENE_BY_BRIGHTNESS(COMMAND_PREFIX, VERSION_2, (byte) 0x06),
            ADJUST_COLOR_TEMP(COMMAND_PREFIX, VERSION_2, (byte) 0x07, bytes -> bytes.length == 2),
            RELATIVE_BRIGHTNESS(COMMAND_PREFIX, VERSION_2, (byte) 0x08),
            ;

            private static final Logger LOG = LoggerFactory.getLogger(Command.class);
            private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

            private final byte prefix;
            private final byte version;
            private final byte opcode;
            private final Predicate<Object[]> validateParams;

            Command(byte prefix, byte version, byte opcode) {
                this(prefix, version, opcode, null);
            }

            Command(byte prefix, byte version, byte opcode, Predicate<Object[]> validateParams) {
                this.prefix = prefix;
                this.version = version;
                this.opcode = opcode;
                this.validateParams = validateParams;
            }

            private String bytesToHex(byte[] bytes) {
                char[] hexChars = new char[bytes.length * 2];

                for (int j = 0; j < bytes.length; j++) {
                    int v = bytes[j] & 0xFF;
                    hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                    hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
                }

                return new String(hexChars);
            }

            /**
             * Builds a byte array to represent the current command in binary form, respecting
             * the Luke Roberts protocol.
             *
             * @param args Parameterized commands can accept parameters, which are appended to the end of the command.
             *             This parameter can be null.
             * @return An array of bytes that correspond to the requested command, with the parameters at the end.
             */
            public byte[] toByteArray(Byte... args) {
                if (validateParams != null && !validateParams.test(args)) {
                    throw new IllegalArgumentException("invalid arguments provided for command " + this);
                }

                byte[] bytes = new byte[3 + (args == null ? 0 : args.length)];

                bytes[0] = prefix;
                bytes[1] = version;
                bytes[2] = opcode;

                if (args != null) {
                    int idx = 0;

                    for (byte b : args) {
                        bytes[3 + idx++] = b;
                    }
                }

                if (LOG.isTraceEnabled()) {
                    LOG.trace("command {} has been converted to binary form: {}", this, bytesToHex(bytes));
                }

                return bytes;
            }
        }

        /**
         * List all the scenes available by default in a Lamp F model.
         * <p>
         * WARN: the user can override or delete the scenes available by default. Except for {@link #DEFAULT_SCENE}
         * and {@link #SHUTDOWN_SCENE} which are always available, there is no guarantee that a scene referenced
         * here will be available in the connected hardware, neither that is hadn't been modified by the user in Luke
         * Roberts' app.
         */
        public enum Scene {
            /**
             * Use this scene to power on the lamp to the default scene.
             */
            DEFAULT_SCENE((byte) 0xFF),

            /**
             * Use this scene to power off the lamp.
             */
            SHUTDOWN_SCENE((byte) 0x00),

            /**
             * Reading scene.
             */
            READING_SCENE((byte) 0x06),

            /**
             * Candle light scene.
             */
            CANDLE_LIGHT_SCENE((byte) 0x04),

            /**
             * Shiny scene.
             */
            SHINY_SCENE((byte) 0x03),

            /**
             * Welcome scene.
             */
            WELCOME_SCENE((byte) 0x01),

            /**
             * Indirect scene.
             */
            INDIRECT_SCENE((byte) 0x05),

            /**
             * Highlights scene.
             */
            HIGHLIGHTS_SCENE((byte) 0x02),

            /**
             * Bright scene.
             */
            BRIGHT_SCENE((byte) 0x07),

            //
            ;

            private final byte id;

            Scene(byte id) {
                this.id = id;
            }

            public byte getId() {
                return id;
            }
        }

        /**
         * Class modeling the configuration structure found in the file luke-roberts-defaults.yaml.
         * Read with SnakeYAML.
         */
        public static final class Config {
            private static final String LUKE_ROBERTS_DEFAULTS = "/config/luke-roberts-defaults.yaml";

            private String mac, localBtAdapter;
            private CustomControlService customControlService;

            public static Config getDefaultConfig() {
                try (InputStream jarConfig = Config.class.getResourceAsStream(LUKE_ROBERTS_DEFAULTS)) {
                    return LukeRoberts.LampF.Config.loadFromStream(jarConfig);
                } catch (IOException e) {
                    throw new IllegalStateException("can not load configuration file from jar file: " + e, e);
                }
            }

            /**
             * WARN : the stream is closed automatically !
             */
            public static Config loadFromStream(InputStream inputStream) {
                try (InputStream is = inputStream) {
                    Constructor c = new Constructor(LukeRoberts.LampF.Config.class);
                    // convert dash-separator to camel-case
                    c.setPropertyUtils(new PropertyUtils() {
                        @Override
                        public Property getProperty(Class<?> type, String name) {
                            String transformed = Arrays.stream(name.split("-"))
                                    .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                                    .collect(Collectors.joining());
                            transformed = Character.toLowerCase(transformed.charAt(0)) + transformed.substring(1);

                            return super.getProperty(type, transformed);
                        }
                    });

                    Yaml yaml = new Yaml(c);
                    return yaml.loadAs(is, LukeRoberts.LampF.Config.class);
                } catch (IOException e) {
                    throw new IllegalStateException("exception during parse of resource: " + e, e);
                }
            }

            public Config merge(Config subconfig) {
                // create an overridden version of the given config
                Config newConfig = new Config();
                newConfig.setLocalBtAdapter(getLocalBtAdapter());
                newConfig.setMac(getMac());

                CustomControlService ccs = new CustomControlService();

                CustomControlService customControlService = getCustomControlService();
                ccs.setUuid(customControlService.getUuid());

                CustomControlService.UserExternalApiEndpoint api = new CustomControlService.UserExternalApiEndpoint();
                ccs.setUserExternalApiEndpoint(api);

                api.setUuid(customControlService.getUserExternalApiEndpoint().getUuid());
                newConfig.setCustomControlService(ccs);

                if (subconfig.getLocalBtAdapter() != null) {
                    newConfig.setLocalBtAdapter(subconfig.getLocalBtAdapter());
                }
                if (subconfig.getMac() != null) {
                    newConfig.setMac(subconfig.getMac());
                }

                return newConfig;
            }

            public String getMac() {
                return mac;
            }

            public void setMac(String mac) {
                this.mac = mac;
            }

            public String getLocalBtAdapter() {
                return localBtAdapter;
            }

            public void setLocalBtAdapter(String localBtAdapter) {
                this.localBtAdapter = localBtAdapter;
            }

            public CustomControlService getCustomControlService() {
                return customControlService;
            }

            public void setCustomControlService(CustomControlService customControlService) {
                this.customControlService = customControlService;
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this)
                        .append("mac", mac)
                        .append("localBtAdapter", localBtAdapter)
                        .append("customControlService", customControlService)
                        .toString();
            }

            public static final class CustomControlService {
                private String uuid;
                private CustomControlService.UserExternalApiEndpoint userExternalApiEndpoint;

                public String getUuid() {
                    return uuid;
                }

                public void setUuid(String uuid) {
                    this.uuid = uuid;
                }

                public UserExternalApiEndpoint getUserExternalApiEndpoint() {
                    return userExternalApiEndpoint;
                }

                public void setUserExternalApiEndpoint(UserExternalApiEndpoint userExternalApiEndpoint) {
                    this.userExternalApiEndpoint = userExternalApiEndpoint;
                }

                @Override
                public String toString() {
                    return new ToStringBuilder(this)
                            .append("uuid", uuid)
                            .append("userExternalApiEndpoint", userExternalApiEndpoint)
                            .toString();
                }

                public static final class UserExternalApiEndpoint {
                    private String uuid;

                    public String getUuid() {
                        return uuid;
                    }

                    public void setUuid(String uuid) {
                        this.uuid = uuid;
                    }

                    @Override
                    public String toString() {
                        return new ToStringBuilder(this)
                                .append("uuid", uuid)
                                .toString();
                    }
                }
            }
        }
    }
}
