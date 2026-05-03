package dev.basarts.cleanchest.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.standard())
            .strict(false)
            .build();

    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Barre de progression optimisée avec StringBuilder
     */
    public static String getProgressBar(int current, int max, int totalBars, String symbol, String colorCompleted, String colorNotCompleted) {
        if (max <= 0) return colorNotCompleted + symbol.repeat(totalBars);

        float percent = (float) current / max;
        int progressBars = (int) (totalBars * percent);

        return colorCompleted + symbol.repeat(Math.max(0, progressBars)) +
                colorNotCompleted + symbol.repeat(Math.max(0, totalBars - progressBars));
    }

    /**
     * Envoi de message direct (évite les conversions inutiles)
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(LEGACY.deserialize(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(LEGACY.deserialize(message));
    }

    /**
     * Traduit les codes de couleur alternatifs (ex: '&') en codes de section '§'.
     *
     * @param altColorChar Le caractère à remplacer (généralement '&')
     * @param textToTranslate Le texte contenant les codes
     * @return Le texte avec les codes de section valides pour Minecraft
     */
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null) return null;

        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static String format(String text, String... replacements) {
        if (text == null) return null;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length && replacements[i] != null) {
                String replacement = replacements[i + 1] == null ? "" : replacements[i + 1];
                text = text.replace(replacements[i], replacement);
            }
        }

        String lower = text.toLowerCase();
        if (lower.contains("<gradient") || lower.contains("<color") || lower.contains("<rainbow") || lower.contains("<#")) {

            String sectionText = translateAlternateColorCodes('&', text);

            Component legacyComp = LEGACY.deserialize(sectionText);
            String mmString = MINI_MESSAGE.serialize(legacyComp)
                    .replace("\\<", "<")
                    .replace("\\>", ">");

            Component finalComp = MINI_MESSAGE.deserialize(mmString);
            return LEGACY.serialize(finalComp);
        }

        return translateAlternateColorCodes('&', text);
    }
}