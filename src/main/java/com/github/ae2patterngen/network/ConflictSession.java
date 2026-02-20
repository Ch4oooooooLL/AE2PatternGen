package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 管理玩家的配方冲突解决会话
 */
public class ConflictSession {

    private static final Map<UUID, ConflictSession> SESSIONS = new HashMap<>();

    public final UUID playerUUID;
    public final String recipeMapId;
    public final List<RecipeEntry> nonConflictingRecipes;
    public final Map<String, List<RecipeEntry>> conflictGroups;
    public final List<String> groupKeys;
    public final Map<String, Integer> selections = new HashMap<>();

    private int currentIndex = 0;

    public ConflictSession(UUID playerUUID, String recipeMapId, List<RecipeEntry> nonConflicting,
        Map<String, List<RecipeEntry>> conflicts) {
        this.playerUUID = playerUUID;
        this.recipeMapId = recipeMapId;
        this.nonConflictingRecipes = nonConflicting;
        this.conflictGroups = conflicts;
        this.groupKeys = new ArrayList<>(conflicts.keySet());
    }

    public static void start(UUID playerUUID, String recipeMapId, List<RecipeEntry> nonConflicting,
        Map<String, List<RecipeEntry>> conflicts) {
        SESSIONS.put(playerUUID, new ConflictSession(playerUUID, recipeMapId, nonConflicting, conflicts));
    }

    public static ConflictSession get(UUID playerUUID) {
        return SESSIONS.get(playerUUID);
    }

    public static void stop(UUID playerUUID) {
        SESSIONS.remove(playerUUID);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalConflicts() {
        return groupKeys.size();
    }

    public String getCurrentProduct() {
        if (currentIndex < 0 || currentIndex >= groupKeys.size()) return null;
        return groupKeys.get(currentIndex);
    }

    public List<RecipeEntry> getCurrentRecipes() {
        String key = getCurrentProduct();
        return key != null ? conflictGroups.get(key) : null;
    }

    public void select(int recipeIndex) {
        String key = getCurrentProduct();
        if (key != null) {
            selections.put(key, recipeIndex);
            currentIndex++;
        }
    }

    public boolean isComplete() {
        return currentIndex >= groupKeys.size();
    }
}
