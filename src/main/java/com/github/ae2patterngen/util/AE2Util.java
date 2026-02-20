package com.github.ae2patterngen.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;

/**
 * 隔离 AE2 API 访问，避免在 Java 17 下因提前类加载导致的 ASM 冲突
 */
public class AE2Util {

    public static boolean tryWirelessConsume(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof IWirelessTermHandler)) {
            return false;
        }

        try {
            return internalWirelessConsume(player, requiredCount, blankPattern, heldItem);
        } catch (Throwable e) {
            // 静默失败，回退到背包消耗
            return false;
        }
    }

    /**
     * 真正的 API 调用逻辑，仅在被 tryWirelessConsume 调用时才会被加载。
     * 这确保了 PacketResolveConflicts 的 Handler 在初始化时不会直接触碰到 AE2 的内部类。
     */
    private static boolean internalWirelessConsume(EntityPlayerMP player, int requiredCount, ItemStack blankPattern,
            ItemStack heldItem) {
        IWirelessTermHandler handler = (IWirelessTermHandler) heldItem.getItem();
        String key = handler.getEncryptionKey(heldItem);
        if (key == null || key.isEmpty())
            return false;

        long serial;
        try {
            serial = Long.parseLong(key);
        } catch (NumberFormatException e) {
            return false;
        }

        Object obj = AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(serial);

        if (obj instanceof IActionHost) {
            IGrid grid = ((IActionHost) obj).getActionableNode()
                    .getGrid();
            if (grid != null) {
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                IMEMonitor<IAEItemStack> inventory = storage.getItemInventory();
                IAEItemStack required = AEApi.instance()
                        .storage()
                        .createItemStack(blankPattern)
                        .setStackSize(requiredCount);

                IAEItemStack available = inventory
                        .extractItems(required, Actionable.SIMULATE, new PlayerSource(player, null));

                if (available != null && available.getStackSize() >= requiredCount) {
                    inventory.extractItems(required, Actionable.MODULATE, new PlayerSource(player, null));
                    player.addChatMessage(
                            new ChatComponentText(EnumChatFormatting.AQUA + "[AE2PatternGen] 已从绑定的 ME 网络中无线提取空白样板。"));
                    return true;
                }
            }
        }
        return false;
    }
}
