package com.yb.dragons.world.museum;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The texts of the museum (signs and floating panels) are always in English, whatever the language of the game:
 * they are written in the world once for every visitor. The translatable texts are turned into plain English text
 * with the English language files of the mod and of Minecraft.
 */
final class MuseumEnglish {
    private static Map<String, String> english;

    private MuseumEnglish() {}

    private static Map<String, String> english() {
        if (english == null) {
            Map<String, String> map = new HashMap<>();
            load(map, "/assets/minecraft/lang/en_us.json");
            load(map, "/assets/bdragon/lang/en_us.json");
            english = map;
        }
        return english;
    }

    private static void load(Map<String, String> map, String path) {
        try (InputStream in = MuseumEnglish.class.getResourceAsStream(path)) {
            if (in != null) Language.loadFromJson(in, map::put);
        } catch (Exception ignored) {
            // the key itself (or the language of the game) is shown instead
        }
    }

    /** The same text with the same styles, in plain English. */
    static Component of(Component text) {
        MutableComponent out;
        if (text.getContents() instanceof TranslatableContents tr) {
            String format = english().get(tr.getKey());
            if (format == null) {
                // not in the English files: left as it is
                out = text.plainCopy();
            } else {
                Object[] args = tr.getArgs();
                Object[] plain = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    plain[i] = args[i] instanceof Component c ? of(c).getString() : String.valueOf(args[i]);
                }
                String s;
                try {
                    s = String.format(Locale.ROOT, format, plain);
                } catch (Exception e) {
                    s = format;
                }
                out = Component.literal(s);
            }
        } else {
            out = text.plainCopy();
        }
        out.setStyle(text.getStyle());
        for (Component sibling : text.getSiblings()) out.append(of(sibling));
        return out;
    }
}
