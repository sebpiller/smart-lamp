package ch.sebpiller.iot.lamp;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorHelperTest {
    @Test
    public void testParseColor() {
        assertThat(ColorHelper.parseColor("white")).containsExactly(0xff, 0xff, 0xff);
        assertThat(ColorHelper.parseColor("black")).containsExactly(0x00, 0x00, 0x00);
        assertThat(ColorHelper.parseColor("red")).containsExactly(0xff, 0x00, 0x00);
    }

}