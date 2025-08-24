package net.mogrul.economy;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class Colours {
    public static Style currencyName = hexToTextColorStyle("#eba534");
    public static Style playerName = hexToTextColorStyle("#b1eb34");
    public static Style mobName = hexToTextColorStyle("#5feb34");

    public static Style success = hexToTextColorStyle("#00FE05");
    public static Style failure = hexToTextColorStyle("#FE0301");
    public static Style itemName = hexToTextColorStyle("#A8FF01");

    public static Style commandTitleName = hexToTextColorStyle("#ecf542");

    private static Style hexToTextColorStyle(String hex) {
        // Remove the '#' character if it exists
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        // Parse the hex string into an RGB integer
        int rgb = Integer.parseInt(hex, 16);
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }
}
