package ch.sebpiller.iot.bluetooth.luke.roberts;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Implements Bluetooth format from Luke Roberts'.
 */
public final class LukeRoberts {
    public static class LampF {
        private static final byte GATT_COMMAND_PREFIX = (byte) 0xA0;
        private static final byte GATT_VERSION_1 = 0x01;
        private static final byte GATT_VERSION_2 = 0x02;
        /**
         * @deprecated as per Luke Roberts' documentation, this characteristics is now deprecated. It was used to change
         * the scene displayed.
         */
        @Deprecated
        static String UUID_CHARACTERISTICS_SELECT_SCENE_SERVICE = "44092844-0567-11e6-b862-0002a5d5c51b";

        public enum Command {
            PING_V1(GATT_COMMAND_PREFIX, GATT_VERSION_1, (byte) 0x00, bytes -> bytes.length == 0),
            PING_V2(GATT_COMMAND_PREFIX, GATT_VERSION_2, (byte) 0x00, bytes -> bytes.length == 0),
            QUERY_SCENE(GATT_COMMAND_PREFIX, GATT_VERSION_1, (byte) 0x01),
            IMMEDIATE_LIGHT(GATT_COMMAND_PREFIX, GATT_VERSION_1, (byte) 0x02),
            BRIGHTNESS(GATT_COMMAND_PREFIX, GATT_VERSION_1, (byte) 0x03, bytes -> bytes.length == 1),
            COLOR_TEMP(GATT_COMMAND_PREFIX, GATT_VERSION_1, (byte) 0x04),
            SELECT_SCENE(GATT_COMMAND_PREFIX, GATT_VERSION_2, (byte) 0x05),
            NEXT_SCENE_BY_BRIGHTNESS(GATT_COMMAND_PREFIX, GATT_VERSION_2, (byte) 0x06),
            ADJUST_COLOR_TEMP(GATT_COMMAND_PREFIX, GATT_VERSION_2, (byte) 0x07, bytes -> bytes.length == 2),
            RELATIVE_BRIGHTNESS(GATT_COMMAND_PREFIX, GATT_VERSION_2, (byte) 0x08),
            ;

            private static final Logger LOG = LoggerFactory.getLogger(Command.class);
            private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

            private final byte prefix;
            private final byte version;
            private final byte opcode;
            private final Optional<Predicate<Object[]>> validateParams;

            Command(byte prefix, byte version, byte opcode) {
                this(prefix, version, opcode, null);
            }

            Command(byte prefix, byte version, byte opcode, Predicate<Object[]> validateParams) {
                this.prefix = prefix;
                this.version = version;
                this.opcode = opcode;
                this.validateParams = Optional.ofNullable(validateParams);
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

            public byte[] toByteArray() {
                return toByteArray((Byte[]) null);
            }

            /**
             * Builds a byte array to represent the current command in binary form, respecting
             * the Luke Roberts protocol.
             *
             * @param lsb Parameterized command can accept parameters, which are appended to the end of the command (LSB).
             *            This parameter can be null.
             * @return An array of bytes that correspond to the requested command, with the parameters at the end.
             */
            public byte[] toByteArray(Byte... lsb) {
                if (validateParams.isPresent() && !validateParams.get().test(lsb)) {
                    throw new IllegalArgumentException("invalid arguments provided");
                }

                byte[] bytes = new byte[3 + (lsb == null ? 0 : lsb.length)];

                bytes[0] = prefix;
                bytes[1] = version;
                bytes[2] = opcode;

                if (lsb != null) {
                    int idx = 0;

                    for (byte b : lsb) {
                        bytes[2 + (idx + 1)] = b;
                        idx++;
                    }
                }

                if (LOG.isTraceEnabled()) {
                    LOG.trace("command {} has been converted to binary form: {}", this, bytesToHex(bytes));
                }

                return bytes;
            }
        }

        public static class LukeRobertsScene {
            public static final byte DEFAULT_SCENE = (byte) 0xFF;
            public static final byte SHUTDOWN_SCENE = 0x00;
        }

        /**
         * Class modeling the configuration structure found in the file luke-roberts-defaults.yaml. Read with SnakeYAML.
         */
        public static final class Config {
            private static final String LUKE_ROBERTS_DEFAULTS = "/config/luke-roberts-defaults.yaml";
            private static final Logger LOG = LoggerFactory.getLogger(Config.class);
            /**
             * The config loaded from the jar. Contains default values for UUIDs and bt adapter. As well as my own lamp MAC :).
             */
            private static Config def;
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
             * Merge an external file located in a path given in a system property (`java -Dxxx`) named #syspropConfigFile
             * with the default configuration file embedded in the jar. If the system property is not defined, the
             * default config is returned.
             */
            public static Config loadOverriddenConfigFromSysprop(String syspropConfigFile) {
                String s = System.getProperty(syspropConfigFile);
                return loadOverriddenConfigFromFile(s);
            }

            public static Config loadOverriddenConfigFromFile(String file) {
                LukeRoberts.LampF.Config config = getDefaultConfig();

                if (isNotBlank(file)) {
                    try (InputStream fsConfig = new FileInputStream(file)) {
                        LukeRoberts.LampF.Config subConfig = LukeRoberts.LampF.Config.loadFromStream(fsConfig);
                        config = config.merge(subConfig);
                    } catch (IOException e) {
                        throw new IllegalStateException("could not load file " + file, e);
                    }
                }

                return config;
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
                        public Property getProperty(Class<? extends Object> type, String name) {
                            String transformed = Arrays.stream(name.split("\\-"))
                                    .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                                    .collect(Collectors.joining());
                            transformed = Character.toLowerCase(transformed.charAt(0)) + transformed.substring(1);

                            return super.getProperty(type, transformed);
                        }
                    });

                    Yaml yaml = new Yaml(c);
                    return yaml.loadAs(is, LukeRoberts.LampF.Config.class);
                } catch (IOException e) {
                    LOG.warn("exception during close: " + e, e);
                    return null;
                }
            }

            public Config merge(Config subconfig) {
                // TODO refactor, crappy code

                // create an overriden version of the given config
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
