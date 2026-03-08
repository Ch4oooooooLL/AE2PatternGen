package com.github.ae2patterngen.network;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.github.ae2patterngen.recipe.RecipeEntry;

public class PacketRecipeConflictBatchTest {

    @Test
    public void maxCandidatesOnlyUsesCurrentBatchGroups() {
        Map<String, List<RecipeEntry>> conflicts = new LinkedHashMap<>();
        conflicts.put("huge-first-group", createRecipes(100));
        conflicts.put("current-batch-a", createRecipes(3));
        conflicts.put("current-batch-b", createRecipes(4));

        ConflictSession session = new ConflictSession(
            UUID.randomUUID(),
            "gt.recipe.assembler",
            new ArrayList<RecipeEntry>(),
            conflicts);

        session.select(0);

        PacketRecipeConflictBatch packet = PacketRecipeConflictBatch.fromSession(session, 2);

        assertEquals(2, packet.recipeGroups.size());
        assertEquals(4, packet.maxCandidatesPerGroup);
    }

    private static List<RecipeEntry> createRecipes(int count) {
        List<RecipeEntry> recipes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            recipes.add(new RecipeEntry("gt", "map", "machine", null, null, null, null, null, 20, 30));
        }
        return recipes;
    }
}
