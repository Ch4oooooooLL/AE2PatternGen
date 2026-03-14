package com.github.ae2patterngen.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class LangKeyCompletenessTest {

    private static final Path EN_US = Paths
        .get("src", "main", "resources", "assets", "ae2patterngen", "lang", "en_US.lang");
    private static final Path ZH_CN = Paths
        .get("src", "main", "resources", "assets", "ae2patterngen", "lang", "zh_CN.lang");

    private static final List<String> REQUIRED_KEYS = Arrays.asList(
        "item.ae2patterngen.pattern_generator.name",
        "ae2patterngen.gui.pattern_gen.title",
        "ae2patterngen.gui.pattern_storage.title",
        "ae2patterngen.gui.pattern_detail.title",
        "ae2patterngen.gui.recipe_picker.title",
        "ae2patterngen.msg.storage.empty_extract",
        "ae2patterngen.msg.generate.no_matching_map",
        "ae2patterngen.msg.cache.missing_or_invalid",
        "ae2patterngen.msg.pattern.generated_and_consumed",
        "ae2patterngen.msg.conflict.cancelled",
        "ae2patterngen.command.help.title",
        "ae2patterngen.command.list.available_maps",
        "ae2patterngen.tooltip.feature.title",
        "ae2patterngen.tooltip.hint.hold_shift");

    @Test
    public void enAndZhMustContainSameKeys() throws IOException {
        Set<String> enKeys = readKeys(EN_US);
        Set<String> zhKeys = readKeys(ZH_CN);
        assertTrue("Language key sets differ between en_US and zh_CN", enKeys.equals(zhKeys));
    }

    @Test
    public void requiredKeysMustExistInBothFiles() throws IOException {
        Set<String> enKeys = readKeys(EN_US);
        Set<String> zhKeys = readKeys(ZH_CN);

        for (String key : REQUIRED_KEYS) {
            assertTrue("Missing key in en_US.lang: " + key, enKeys.contains(key));
            assertTrue("Missing key in zh_CN.lang: " + key, zhKeys.contains(key));
        }
    }

    private static Set<String> readKeys(Path path) throws IOException {
        Set<String> keys = new LinkedHashSet<String>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            keys.add(
                line.substring(0, idx)
                    .trim());
        }
        return keys;
    }
}
