package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.storage.PatternStorage;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端: 仓储操作 (取出到背包 / 清空 / 删除单条)
 */
public class PacketStorageAction implements IMessage {

    /** 0 = 取出到背包, 1 = 清空, 2 = 删除单条 */
    private int action;

    /** 用于 ACTION_DELETE: 要删除的索引 */
    private int targetIndex;

    public PacketStorageAction() {}

    public PacketStorageAction(int action) {
        this.action = action;
        this.targetIndex = -1;
    }

    public PacketStorageAction(int action, int targetIndex) {
        this.action = action;
        this.targetIndex = targetIndex;
    }

    public static final int ACTION_EXTRACT = 0;
    public static final int ACTION_CLEAR = 1;
    public static final int ACTION_DELETE = 2;

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        targetIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(targetIndex);
    }

    public static class Handler implements IMessageHandler<PacketStorageAction, IMessage> {

        @Override
        public IMessage onMessage(PacketStorageAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();

            if (message.action == ACTION_EXTRACT) {
                handleExtract(player, uuid);
            } else if (message.action == ACTION_CLEAR) {
                handleClear(player, uuid);
            } else if (message.action == ACTION_DELETE) {
                handleDelete(player, uuid, message.targetIndex);
            }
            return null;
        }

        private void handleExtract(EntityPlayerMP player, UUID uuid) {
            if (PatternStorage.isEmpty(uuid)) {
                player
                    .addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 仓储为空，无可取出的样板"));
                return;
            }

            // 计算背包可用空间
            int freeSlots = 0;
            for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                if (player.inventory.mainInventory[i] == null) {
                    freeSlots++;
                }
            }

            if (freeSlots == 0) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 背包已满，无法取出样板"));
                return;
            }

            List<ItemStack> extracted = PatternStorage.extract(uuid, freeSlots);
            int added = 0;
            for (ItemStack stack : extracted) {
                if (player.inventory.addItemStackToInventory(stack)) {
                    added++;
                }
            }

            player.inventoryContainer.detectAndSendChanges();
            player.sendContainerToPlayer(player.inventoryContainer);

            PatternStorage.StorageSummary remaining = PatternStorage.getSummary(uuid);
            String msg = EnumChatFormatting.GREEN + "[AE2PatternGen] 已取出 " + added + " 个样板到背包";
            if (remaining.count > 0) {
                msg += EnumChatFormatting.GRAY + " (剩余 " + remaining.count + " 个)";
            }
            player.addChatMessage(new ChatComponentText(msg));
        }

        private void handleClear(EntityPlayerMP player, UUID uuid) {
            PatternStorage.StorageSummary summary = PatternStorage.getSummary(uuid);
            if (summary.count == 0) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 仓储已为空"));
                return;
            }

            PatternStorage.clear(uuid);
            player.addChatMessage(
                new ChatComponentText(EnumChatFormatting.GREEN + "[AE2PatternGen] 已清空 " + summary.count + " 个样板"));
        }

        private void handleDelete(EntityPlayerMP player, UUID uuid, int index) {
            ItemStack removed = PatternStorage.delete(uuid, index);
            if (removed == null) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "[AE2PatternGen] 删除失败: 索引无效"));
                return;
            }

            PatternStorage.StorageSummary remaining = PatternStorage.getSummary(uuid);
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "[AE2PatternGen] 已删除: "
                        + removed.getDisplayName()
                        + EnumChatFormatting.GRAY
                        + " (剩余 "
                        + remaining.count
                        + " 个)"));
        }
    }
}
