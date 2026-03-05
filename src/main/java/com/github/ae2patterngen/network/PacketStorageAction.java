package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.util.I18nUtil;

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
                send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.storage.empty_extract");
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
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.storage.inventory_full");
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
            if (remaining.count > 0) {
                send(
                    player,
                    EnumChatFormatting.GREEN,
                    "ae2patterngen.msg.storage.extracted_with_remaining",
                    added,
                    remaining.count);
            } else {
                send(player, EnumChatFormatting.GREEN, "ae2patterngen.msg.storage.extracted", added);
            }
        }

        private void handleClear(EntityPlayerMP player, UUID uuid) {
            PatternStorage.StorageSummary summary = PatternStorage.getSummary(uuid);
            if (summary.count == 0) {
                send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.storage.already_empty");
                return;
            }

            PatternStorage.clear(uuid);
            send(player, EnumChatFormatting.GREEN, "ae2patterngen.msg.storage.cleared", summary.count);
        }

        private void handleDelete(EntityPlayerMP player, UUID uuid, int index) {
            ItemStack removed = PatternStorage.delete(uuid, index);
            if (removed == null) {
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.storage.delete_invalid");
                return;
            }

            PatternStorage.StorageSummary remaining = PatternStorage.getSummary(uuid);
            send(
                player,
                EnumChatFormatting.GREEN,
                "ae2patterngen.msg.storage.deleted",
                removed.getDisplayName(),
                remaining.count);
        }

        private void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
            player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
        }
    }
}
