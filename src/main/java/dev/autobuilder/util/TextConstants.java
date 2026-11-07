package dev.autobuilder.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TextConstants {
    public static final String PREFIX = "§8[§bAutoBuilder§8] §r";

    private TextConstants() {}

    public static MutableText prefix() {
        return Text.literal("[AutoBuilder] ").formatted(Formatting.DARK_AQUA, Formatting.BOLD);
    }

    public static MutableText message(String text, Formatting formatting) {
        return prefix().append(Text.literal(text).formatted(formatting));
    }

    public static MutableText info(String text) {
        return prefix().append(Text.literal(text).formatted(Formatting.AQUA));
    }

    public static MutableText success(String text) {
        return prefix().append(Text.literal(text).formatted(Formatting.GREEN));
    }

    public static MutableText warning(String text) {
        return prefix().append(Text.literal(text).formatted(Formatting.GOLD));
    }

    public static MutableText error(String text) {
        return prefix().append(Text.literal(text).formatted(Formatting.RED));
    }
}
