package com.github.ae2patterngen.util;

import net.minecraft.util.StatCollector;

public final class I18nUtil {

    interface Translator {

        String translate(String key);

        String translateFormatted(String key, Object... args);
    }

    private static final Translator DEFAULT_TRANSLATOR = new StatCollectorTranslator();
    private static volatile Translator translator = DEFAULT_TRANSLATOR;

    private I18nUtil() {}

    public static String tr(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String translated = translator.translate(key);
        if (translated == null || translated.isEmpty()) {
            return key;
        }
        return translated;
    }

    public static String tr(String key, Object... args) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (args == null || args.length == 0) {
            return tr(key);
        }
        String translated = translator.translateFormatted(key, args);
        if (translated == null || translated.isEmpty()) {
            return key;
        }
        return translated;
    }

    public static String trOr(String key, String fallback, Object... args) {
        String translated = tr(key, args);
        if (!isMissingTranslation(key, translated)) {
            return translated;
        }
        return formatFallback(fallback, args);
    }

    static void setTranslatorForTest(Translator testTranslator) {
        translator = testTranslator != null ? testTranslator : DEFAULT_TRANSLATOR;
    }

    static void resetTranslatorForTest() {
        translator = DEFAULT_TRANSLATOR;
    }

    private static boolean isMissingTranslation(String key, String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return key != null && key.equals(value);
    }

    private static String formatFallback(String fallback, Object... args) {
        if (fallback == null || fallback.isEmpty()) {
            return "";
        }
        if (args == null || args.length == 0) {
            return fallback;
        }
        try {
            return String.format(fallback, args);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static final class StatCollectorTranslator implements Translator {

        @Override
        public String translate(String key) {
            try {
                return StatCollector.translateToLocal(key);
            } catch (RuntimeException ignored) {
                return key;
            }
        }

        @Override
        public String translateFormatted(String key, Object... args) {
            if (args == null || args.length == 0) {
                return translate(key);
            }
            try {
                return StatCollector.translateToLocalFormatted(key, args);
            } catch (RuntimeException ignored) {
                return translate(key);
            }
        }
    }
}
