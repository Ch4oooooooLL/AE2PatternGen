package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.ae2patterngen.recipe.RecipeEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: 批量发送冲突配方组。
 */
public class PacketRecipeConflictBatch implements IMessage {

    /** 本批次起始冲突索引 (1-based) */
    public int startIndex;
    public int totalConflicts;
    /** 当前会话中单组最大候选数量，用于客户端一次性分配行数，减少切批重建窗口闪烁。 */
    public int maxCandidatesPerGroup;
    public List<String> productNames;
    public List<List<RecipeEntry>> recipeGroups;

    public PacketRecipeConflictBatch() {}

    public PacketRecipeConflictBatch(int startIndex, int totalConflicts, int maxCandidatesPerGroup,
        List<String> productNames, List<List<RecipeEntry>> recipeGroups) {
        this.startIndex = startIndex;
        this.totalConflicts = totalConflicts;
        this.maxCandidatesPerGroup = maxCandidatesPerGroup;
        this.productNames = productNames;
        this.recipeGroups = recipeGroups;
    }

    public static PacketRecipeConflictBatch fromSession(ConflictSession session, int maxGroups) {
        int safeMax = Math.max(1, maxGroups);
        int start = session.getCurrentIndex();
        int end = Math.min(session.getTotalConflicts(), start + safeMax);
        int maxCandidates = 0;

        List<String> names = new ArrayList<>();
        List<List<RecipeEntry>> groups = new ArrayList<>();
        for (String key : session.groupKeys) {
            List<RecipeEntry> allRecipes = session.conflictGroups.get(key);
            if (allRecipes != null && allRecipes.size() > maxCandidates) {
                maxCandidates = allRecipes.size();
            }
        }
        for (int i = start; i < end; i++) {
            String key = session.groupKeys.get(i);
            names.add(key != null ? key : "Unknown");
            List<RecipeEntry> recipes = session.conflictGroups.get(key);
            groups.add(recipes != null ? recipes : new ArrayList<RecipeEntry>());
        }

        return new PacketRecipeConflictBatch(
            start + 1,
            session.getTotalConflicts(),
            Math.max(1, maxCandidates),
            names,
            groups);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        startIndex = buf.readInt();
        totalConflicts = buf.readInt();
        maxCandidatesPerGroup = buf.readInt();
        int groupCount = buf.readInt();

        productNames = new ArrayList<>(groupCount);
        recipeGroups = new ArrayList<>(groupCount);

        for (int i = 0; i < groupCount; i++) {
            String name = ByteBufUtils.readUTF8String(buf);
            productNames.add(name);

            int recipeCount = buf.readInt();
            List<RecipeEntry> recipes = new ArrayList<>(recipeCount);
            for (int r = 0; r < recipeCount; r++) {
                recipes.add(PacketRecipeConflicts.readRecipe(buf));
            }
            recipeGroups.add(recipes);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(startIndex);
        buf.writeInt(totalConflicts);
        buf.writeInt(Math.max(1, maxCandidatesPerGroup));

        List<String> safeNames = productNames != null ? productNames : new ArrayList<String>();
        List<List<RecipeEntry>> safeGroups = recipeGroups != null ? recipeGroups : new ArrayList<List<RecipeEntry>>();
        int count = Math.min(safeNames.size(), safeGroups.size());
        buf.writeInt(count);

        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeUTF8String(buf, safeNames.get(i) != null ? safeNames.get(i) : "Unknown");
            List<RecipeEntry> recipes = safeGroups.get(i) != null ? safeGroups.get(i) : new ArrayList<RecipeEntry>();
            buf.writeInt(recipes.size());
            for (RecipeEntry recipe : recipes) {
                PacketRecipeConflicts.writeRecipe(buf, recipe);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketRecipeConflictBatch, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRecipeConflictBatch message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> com.github.ae2patterngen.gui.GuiRecipePicker.openBatch(message));
            return null;
        }
    }
}
