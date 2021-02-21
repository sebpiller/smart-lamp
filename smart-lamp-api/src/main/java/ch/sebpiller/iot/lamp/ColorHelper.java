package ch.sebpiller.iot.lamp;

public class ColorHelper {

    /**
     * Parses a color either from its name (in english or french) or rgb bytes in hex.
     *
     * @param v for example "WHITE" or "FFFFFF".
     * @return an array of length 3, containing a color in RGB. Never null (fallback to white).
     */
    public static int[] parseColor(String v) {
        if (v != null) {
            String lower = v.toLowerCase();

            switch (lower) {
                case "white":
                case "blanc":
                    return new int[]{0xff, 0xff, 0xff};
                case "black":
                case "noir":
                    return new int[]{0x00, 0x00, 0x00};
                //
                case "red":
                case "rouge":
                    return new int[]{0xff, 0x00, 0x00};
                case "green":
                case "vert":
                    return new int[]{0x00, 0xff, 0x00};
                case "blue":
                case "bleu":
                    return new int[]{0x00, 0x00, 0xff};
                //
                case "magenta":
                    return new int[]{0xff, 0x00, 0xff};
                case "cyan":
                    return new int[]{0x00, 0xff, 0xff};
                case "yellow":
                case "jaune":
                    return new int[]{0xff, 0xff, 0x00};
                // more color names from here: https://simple.wikipedia.org/wiki/List_of_colors
                case "amethyst":
                    return new int[]{0x99, 0x66, 0xcc};
                case "aquamarine":
                    return new int[]{0x7f, 0xff, 0xd4};
                case "bronze":
                    return new int[]{0xcd, 0x7f, 0x32};
                case "cherry":
                case "cerise":
                    return new int[]{0xde, 0x31, 0x63};
                case "coffee":
                case "cafe":
                    return new int[]{0x6f, 0x4e, 0x37};
                case "copper":
                case "cuivre":
                    return new int[]{0xB8, 0x73, 0x33};
                case "emerald":
                case "emeraude":
                    return new int[]{0x50, 0xC8, 0x78};
                // TODO recognize more color names
            }

            if (lower.matches("[0-9a-f]{6}")) {
                return new int[]{
                        Integer.parseInt(v.substring(0, 2), 16),
                        Integer.parseInt(v.substring(2, 4), 16),
                        Integer.parseInt(v.substring(4, 6), 16),
                };
            }
        }

        // white by default when everything else has failed.
        return new int[]{0xff, 0xff, 0xff};
    }
}
