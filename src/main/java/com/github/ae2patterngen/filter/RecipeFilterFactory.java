package com.github.ae2patterngen.filter;

/**
 * Builds the shared recipe filter pipeline used by GUI preview, commands, and network requests.
 */
public final class RecipeFilterFactory {

    private RecipeFilterFactory() {}

    public static CompositeFilter build(String outputOreDict, String inputOreDict, String ncItem, String blacklistInput,
        String blacklistOutput, int targetTier) {
        CompositeFilter filter = new CompositeFilter();

        if (isEnabled(outputOreDict)) {
            filter.addFilter(new OutputOreDictFilter(outputOreDict));
        }
        if (isEnabled(inputOreDict)) {
            filter.addFilter(new InputOreDictFilter(inputOreDict));
        }
        if (isEnabled(ncItem)) {
            filter.addFilter(new NCItemFilter(ncItem));
        }
        if (isEnabled(blacklistInput)) {
            filter.addFilter(new BlacklistFilter(blacklistInput, true, false));
        }
        if (isEnabled(blacklistOutput)) {
            filter.addFilter(new BlacklistFilter(blacklistOutput, false, true));
        }
        if (targetTier >= 0) {
            filter.addFilter(new TierFilter(targetTier));
        }

        return filter;
    }

    private static boolean isEnabled(String value) {
        return value != null && !value.isEmpty() && !"*".equals(value);
    }
}
