package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.PatternEncoder;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.util.AE2Util;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: 提交冲突决策
 */
public class PacketResolveConflicts implements IMessage {

    public int recipeIndex;
    public boolean cancel;

    public PacketResolveConflicts() {
    }

    public PacketResolveConflicts(int recipeIndex, boolean cancel) {
        this.recipeIndex = recipeIndex;
        this.cancel = cancel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeIndex = buf.readInt();
        cancel = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(recipeIndex);
        buf.writeBoolean(cancel);
    }

    public static class Handler implements IMessageHandler<PacketResolveConflicts, IMessage> {

        @Override
        public IMessage onMessage(PacketResolveConflicts message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            ConflictSession session = ConflictSession.get(uuid);

            if (session == null)
                return null;

            if (message.cancel) {
                ConflictSession.stop(uuid);
                player.addChatMessage(
                        new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 放弃本次生成,可进行更详细的筛选避免重复样板"));
                return null;
            }

            session.select(message.recipeIndex);

            if (session.isComplete()) {
                // 执行最终生成逻辑
                finalizeGeneration(player, session);
                ConflictSession.stop(uuid);
            } else {
                // 发送下一个冲突
                PacketRecipeConflicts nextPacket = new PacketRecipeConflicts(
                        session.getCurrentProduct(),
                        session.getCurrentIndex() + 1,
                        session.getTotalConflicts(),
                        session.getCurrentRecipes());
                NetworkHandler.INSTANCE.sendTo(nextPacket, player);
            }

            return null;
        }

        private void finalizeGeneration(EntityPlayerMP player, ConflictSession session) {
            List<RecipeEntry> finalRecipes = new ArrayList<>(session.nonConflictingRecipes);
            for (String key : session.groupKeys) {
                Integer index = session.selections.get(key);
                if (index != null) {
                    finalRecipes.add(
                            session.conflictGroups.get(key)
                                    .get(index));
                }
            }

            // 复制 PacketGeneratePatterns 的后续逻辑 (编码、扣除、存入)
            // 这里应该重构一下代码以复用，但为了快速实现先直接搬运

            // 6. 编码样板
            List<ItemStack> patterns = PatternEncoder.encodeBatch(finalRecipes);
            if (patterns.isEmpty()) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 编码后无有效样板"));
                return;
            }

            // 7. 结算消耗
            int requiredCount = patterns.size();
            ItemStack blankPattern = com.github.ae2patterngen.util.InventoryUtil.getBlankPattern();
            boolean consumed = performConsumption(player, requiredCount, blankPattern);

            if (!consumed)
                return;

            // 8. 写入虚拟仓储
            PatternStorage.save(player.getUniqueID(), patterns, session.recipeMapId);

            player.addChatMessage(
                    new ChatComponentText(
                            EnumChatFormatting.GREEN + "[AE2PatternGen] 已扣除 " + requiredCount + " 个空白样板并生成了等量成品。"));
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "成品已存入仓储! 蹲下右键空气查看，蹲下右键容器导出。"));
        }

        private boolean performConsumption(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
            // 优先尝试从 AE2 网络无线提取 (已隔离到 AE2Util)
            boolean consumed = AE2Util.tryWirelessConsume(player, requiredCount, blankPattern);

            if (!consumed) {
                if (!com.github.ae2patterngen.util.InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
                    int currentHas = com.github.ae2patterngen.util.InventoryUtil.countItem(player, blankPattern);
                    player.addChatMessage(
                            new ChatComponentText(
                                    EnumChatFormatting.RED + "[AE2PatternGen] 生成失败: 空白样板不足。需要 "
                                            + requiredCount
                                            + " 但只有 "
                                            + currentHas));
                    return false;
                }
                consumed = true;
            }
            return consumed;
        }
    }
}
