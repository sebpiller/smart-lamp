package ch.sebpiller.iot.bluetooth.lamp.luke.roberts;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LukeRobertsTest {
    private static final Logger LOG = LoggerFactory.getLogger(LukeRobertsTest.class);

    @Test
    public void testCommandsByteArrayConversion() {
        assertThat(LukeRoberts.LampF.Command.BRIGHTNESS.toByteArray((byte) 0)).isEqualTo(new byte[]{
                (byte) 0xA0, 0x01, 0x03, 0x00});
        assertThat(LukeRoberts.LampF.Command.BRIGHTNESS.toByteArray((byte) 0x7F)).isEqualTo(new byte[]{
                (byte) 0xA0, 0x01, 0x03, 0x7F});
        assertThat(LukeRoberts.LampF.Command.BRIGHTNESS.toByteArray((byte) 0xAD)).isEqualTo(new byte[]{
                (byte) 0xA0, 0x01, 0x03, (byte) 0xAD});
    }

    @Test
    public void testConfigCanBeLoaded() {
        LukeRoberts.LampF.Config defaultConfig = LukeRoberts.LampF.Config.getDefaultConfig();

        assertThat(defaultConfig).isNotNull();
        assertThat(defaultConfig.getMac()).isEqualTo("C4:AC:05:42:73:A4");
        assertThat(defaultConfig.getLocalBtAdapter()).isEqualTo("hci0");

        assertThat(defaultConfig.getCustomControlService()).isNotNull();
        assertThat(defaultConfig.getCustomControlService().getUuid()).isNotNull().isNotEmpty();

        assertThat(defaultConfig.getCustomControlService().getUserExternalApiEndpoint()).isNotNull();
        assertThat(defaultConfig.getCustomControlService().getUserExternalApiEndpoint().getUuid()).isNotNull().isNotEmpty();

        LukeRoberts.LampF.Config tests = LukeRoberts.LampF.Config.loadFromStream(getClass().getResourceAsStream("/luke-roberts-lamp-f-mac-FF.yaml"));
        LukeRoberts.LampF.Config merged = defaultConfig.merge(tests);

        LOG.info("default config is: {}", defaultConfig);
        LOG.info("tests config is: {}", tests);
        LOG.info("merged config is: {}", merged);

        assertThat(merged.getLocalBtAdapter()).isEqualTo("hci27");
        assertThat(merged.getMac()).isEqualTo("FF:FF:FF:FF:FF:FF");
    }
}