package com.github.ae2patterngen.network;

import com.github.ae2patterngen.config.ForgeConfig;

/**
 * Protects the client/server from opening interactive conflict selection for
 * oversized result sets that are not practical to resolve manually.
 */
public final class ConflictSelectionPolicy {

    private ConflictSelectionPolicy() {}

    public static boolean shouldAbortInteractiveSelection(int filteredRecipeCount, int conflictGroupCount) {
        return filteredRecipeCount > ForgeConfig.getMaxFilteredRecipes()
            || conflictGroupCount > ForgeConfig.getMaxConflictGroups();
    }

    public static int getMaxInteractiveFilteredRecipes() {
        return ForgeConfig.getMaxFilteredRecipes();
    }

    public static int getMaxInteractiveConflictGroups() {
        return ForgeConfig.getMaxConflictGroups();
    }
}
