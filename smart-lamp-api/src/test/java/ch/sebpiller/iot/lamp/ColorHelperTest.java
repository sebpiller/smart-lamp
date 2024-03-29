package ch.sebpiller.iot.lamp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ColorHelperTest {
    @Test
    public void testParseColor() {
        assertThat(ColorHelper.parseColor("white")).containsExactly(0xff, 0xff, 0xff);
        assertThat(ColorHelper.parseColor("black")).containsExactly(0x00, 0x00, 0x00);
        assertThat(ColorHelper.parseColor("red")).containsExactly(0xff, 0x00, 0x00);
        assertThat(ColorHelper.parseColor("green")).containsExactly(0x00, 0xff, 0x00);
        assertThat(ColorHelper.parseColor("blue")).containsExactly(0x00, 0x00, 0xff);

        assertThat(ColorHelper.parseColor("invalid_value")).containsExactly(0xff, 0xff, 0xff);
    }

}

