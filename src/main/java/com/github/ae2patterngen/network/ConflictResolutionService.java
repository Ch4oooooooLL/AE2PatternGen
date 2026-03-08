package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.ae2patterngen.config.ForgeConfig;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 冲突会话服务: 统一批次下发、最终结果收敛与生成触发。
 */
public final class ConflictResolutionService {

    private ConflictResolutionService() {}

    public static int currentServerStartIndex(ConflictSession session) {
        return session.getCurrentIndex() + 1;
    }

    public static void sendCurrentBatch(EntityPlayerMP player, ConflictSession session) {
        PacketRecipeConflictBatch batchPacket = PacketRecipeConflictBatch
            .fromSession(session, ForgeConfig.getConflictBatchSize());
        NetworkHandler.INSTANCE.sendTo(batchPacket, player);
    }

    public static List<RecipeEntry> collectFinalRecipes(ConflictSession session) {
        List<RecipeEntry> finalRecipes = new ArrayList<>(session.nonConflictingRecipes);
        for (String key : session.groupKeys) {
            Integer index = session.selections.get(key);
            if (index != null) {
                finalRecipes.add(
                    session.conflictGroups.get(key)
                        .get(index));
            }
        }
        return finalRecipes;
    }

    public static void finalizeSession(EntityPlayerMP player, ConflictSession session) {
        List<RecipeEntry> finalRecipes = collectFinalRecipes(session);
        PatternGenerationService.generateAndStore(player, session.recipeMapId, finalRecipes);
    }
}
