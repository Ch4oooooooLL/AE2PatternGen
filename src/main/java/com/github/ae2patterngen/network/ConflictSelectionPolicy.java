package com.github.ae2patterngen.network;

/**
 * Protects the client/server from opening interactive conflict selection for
 * oversized result sets that are not practical to resolve manually.
 */
public final class ConflictSelectionPolicy {

    static final int MAX_INTERACTIVE_FILTERED_RECIPES = 4096;
    static final int MAX_INTERACTIVE_CONFLICT_GROUPS = 256;

    private ConflictSelectionPolicy() {}

    public static boolean shouldAbortInteractiveSelection(int filteredRecipeCount, int conflictGroupCount) {
        return filteredRecipeCount > MAX_INTERACTIVE_FILTERED_RECIPES
            || conflictGroupCount > MAX_INTERACTIVE_CONFLICT_GROUPS;
    }

    public static int getMaxInteractiveFilteredRecipes() {
        return MAX_INTERACTIVE_FILTERED_RECIPES;
    }

    public static int getMaxInteractiveConflictGroups() {
        return MAX_INTERACTIVE_CONFLICT_GROUPS;
    }
}
