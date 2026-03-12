package com.github.ae2patterngen.filter;

/**
 * Builds the shared recipe filter pipeline used by GUI preview, commands, and network requests.
 */
public final class RecipeFilterFactory {

    private RecipeFilterFactory() {}

    public static CompositeFilter build(String outputOreDict, String inputOreDict, String ncItem, String blacklistInput,
        String blacklistOutput, int targetTier) {
        CompositeFilter filter = new CompositeFilter();
        ExplicitStackMatcher.StackMatchCache stackMatchCache = new ExplicitStackMatcher.StackMatchCache();

        if (targetTier >= 0) {
            filter.addFilter(new TierFilter(targetTier));
        }

        if (isEnabled(outputOreDict)) {
            filter.addFilter(new OutputOreDictFilter(outputOreDict, stackMatchCache));
        }
        if (isEnabled(inputOreDict)) {
            filter.addFilter(new InputOreDictFilter(inputOreDict, stackMatchCache));
        }
        if (isEnabled(ncItem)) {
            filter.addFilter(new NCItemFilter(ncItem, stackMatchCache));
        }
        if (isEnabled(blacklistInput)) {
            filter.addFilter(new BlacklistFilter(blacklistInput, true, false, stackMatchCache));
        }
        if (isEnabled(blacklistOutput)) {
            filter.addFilter(new BlacklistFilter(blacklistOutput, false, true, stackMatchCache));
        }

        return filter;
    }

    private static boolean isEnabled(String value) {
        return value != null && !value.isEmpty() && !"*".equals(value);
    }
}
