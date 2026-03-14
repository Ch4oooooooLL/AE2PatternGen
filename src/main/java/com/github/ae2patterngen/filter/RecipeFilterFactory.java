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

        // Priority 1: TierFilter (extremely low cost, can quickly reject most recipes)
        if (targetTier >= 0) {
            filter.addFilter(new TierFilter(targetTier));
        }

        // Priority 2: Blacklist filters (typically have few rules, fast matching)
        if (isEnabled(blacklistInput)) {
            filter.addFilter(new BlacklistFilter(blacklistInput, true, false, stackMatchCache));
        }
        if (isEnabled(blacklistOutput)) {
            filter.addFilter(new BlacklistFilter(blacklistOutput, false, true, stackMatchCache));
        }

        // Priority 3: NC filter (typically has few rules)
        if (isEnabled(ncItem)) {
            filter.addFilter(new NCItemFilter(ncItem, stackMatchCache));
        }

        // Priority 4: Output ore dict filter (highest cost, most rules typically)
        if (isEnabled(outputOreDict)) {
            filter.addFilter(new OutputOreDictFilter(outputOreDict, stackMatchCache));
        }

        // Priority 5: Input ore dict filter (highest cost, most rules typically)
        if (isEnabled(inputOreDict)) {
            filter.addFilter(new InputOreDictFilter(inputOreDict, stackMatchCache));
        }

        return filter;
    }

    private static boolean isEnabled(String value) {
        return value != null && !value.isEmpty() && !"*".equals(value);
    }
}
