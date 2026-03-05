package com.github.ae2patterngen.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

public class I18nUtilTest {

    @After
    public void tearDown() {
        I18nUtil.resetTranslatorForTest();
    }

    @Test
    public void trReturnsMappedText() {
        I18nUtil.setTranslatorForTest(new MapTranslator(mapOf("gui.title", "Pattern Generator")));
        assertEquals("Pattern Generator", I18nUtil.tr("gui.title"));
    }

    @Test
    public void trOrFallsBackWhenKeyMissing() {
        I18nUtil.setTranslatorForTest(new MapTranslator(mapOf("exists", "value")));
        assertEquals("fallback", I18nUtil.trOr("missing.key", "fallback"));
    }

    @Test
    public void trSupportsFormatting() {
        I18nUtil.setTranslatorForTest(new MapTranslator(mapOf("msg.count", "Count: %s")));
        assertEquals("Count: 3", I18nUtil.tr("msg.count", 3));
    }

    private static Map<String, String> mapOf(String key, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        return map;
    }

    private static final class MapTranslator implements I18nUtil.Translator {

        private final Map<String, String> values;

        private MapTranslator(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public String translate(String key) {
            String value = values.get(key);
            return value != null ? value : key;
        }

        @Override
        public String translateFormatted(String key, Object... args) {
            String template = values.get(key);
            if (template == null) {
                return key;
            }
            return String.format(template, args);
        }
    }
}
