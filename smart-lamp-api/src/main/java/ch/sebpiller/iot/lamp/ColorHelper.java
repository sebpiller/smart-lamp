package ch.sebpiller.iot.lamp;

public class ColorHelper {

    /**
     * Parses a color either from its name (in english) or rgb bytes in hex.
     *
     * @param value for example "WHITE" or "FFFFFF".
     * @return an array of length 3, containing a color in RGB. Never null (fallback to white).
     */
    public static int[] parseColor(String value) {
        if (value != null) {
            switch (value.toLowerCase()) {
                case "white":
                    return new int[]{0xff, 0xff, 0xff};
                case "black":
                    return new int[]{0x00, 0x00, 0x00};
                //
                case "red":
                    return new int[]{0xff, 0x00, 0x00};
                case "green":
                    return new int[]{0x00, 0xff, 0x00};
                case "blue":
                    return new int[]{0x00, 0x00, 0xff};
                //
                case "magenta":
                    return new int[]{0xff, 0x00, 0xff};
                case "cyan":
                    return new int[]{0x00, 0xff, 0xff};
                case "yellow":
                    return new int[]{0xff, 0xff, 0x00};

                // TODO recognize more color names
            }

            if (value.matches("[0-9a-f]{6}")) {
                return new int[]{
                        Integer.parseInt(value.substring(0, 2), 16),
                        Integer.parseInt(value.substring(2, 4), 16),
                        Integer.parseInt(value.substring(4, 6), 16),
                };
            }
        }

        // fallback to white
        return new int[]{
                0xff, 0xff, 0xff // white by default when everything else has failed.
        };
    }
}
